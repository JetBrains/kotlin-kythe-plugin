/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kythe.indexer

import com.google.devtools.kythe.analyzers.base.EdgeKind
import com.google.devtools.kythe.analyzers.base.FactEmitter
import com.google.devtools.kythe.analyzers.base.NodeKind
import com.google.devtools.kythe.platform.shared.StatisticsCollector
import com.google.devtools.kythe.proto.Analysis
import com.google.devtools.kythe.proto.Storage
import com.google.devtools.kythe.util.Span
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.kythe.signatures.SignaturesProvider
import org.jetbrains.kotlin.kythe.signatures.isImplicit
import java.lang.UnsupportedOperationException


/** TODO-list:
 * Kotlin-related:
 * - annotations
 * - type aliases
 * - lambdas
 * - delegation
 * - callable references
 * - functional types
 * - REF edges from type arguments
 *
 * Kythe-related:
 * - proper corpus/path/root
 * - review mapping 'Kotlin kind -> NodeKind'
 * - sources (MarkedSource)
 * - decide on representation of modules and packages
 *
 * Implementation-related:
 * - relax assertions (do not fail on unexpected code constructs)
 */
class IrBasedKytheIndexer(
    compilationVName: Storage.VName,
    factEmitter: FactEmitter,
    statisticsCollector: StatisticsCollector,
    requiredInputs: MutableList<Analysis.CompilationUnit.FileInput>,
    private val sourcesManager: SourcesManager,
    signaturesProvider: SignaturesProvider
) : IrElementVisitor<Storage.VName?, Nothing?> {
    private val visited: HashMap<IrElement, Storage.VName> = hashMapOf()
    private val visitedTypes: HashMap<IrType, Storage.VName> = hashMapOf()
    private val kotlinEntrySets = KotlinEntrySets(
        statisticsCollector,
        factEmitter,
        compilationVName,
        requiredInputs,
        signaturesProvider
    )

    // TODO: hack; remove and get currentFile properly
    private var currentFile: IrFile? = null


    fun indexIrTree(root: IrElement) {
        root.accept(this, null)
    }


    // ======== Top-level ========

    override fun visitElement(element: IrElement, data: Nothing?): Storage.VName {
        throw IllegalStateException("Unsupported IrElement: $element")
    }

    override fun visitModuleFragment(declaration: IrModuleFragment, data: Nothing?): Storage.VName? {
        declaration.files.forEach {
            visitFile(it, data)
        }

//        declaration.dependencyModules.forEach { visitModuleFragment(it, data) }
//        declaration.externalPackageFragments.forEach { visitExternalPackageFragment(it, data) }
        return null
    }

    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment, data: Nothing?): Storage.VName? {
        declaration.declarations.forEach {
            it.accept(this, data)
        }

        return null
    }

    override fun visitFile(declaration: IrFile, data: Nothing?): Storage.VName {
        val fileVName = sourcesManager.fileVName(declaration)
        kotlinEntrySets.newFileNodeAndEmit(
            fileVName, sourcesManager.fileContent(declaration), sourcesManager.fileCharset(declaration)
        )

        currentFile = declaration // Set current file for child declarations
        for (childDeclaration in declaration.declarations) {
            childDeclaration.accept(this, data)?.emitChildOf(fileVName)
        }
        currentFile = null

        return fileVName
    }


    // ======== Declarations ========

    override fun visitClass(declaration: IrClass, data: Nothing?): Storage.VName {
        return visited.getOrPut(declaration) {
            kotlinEntrySets.enterScope(declaration)

            val classVName = processDeclaration(declaration) { declarationVName ->
                for (member in declaration.declarations) {
                    member.accept(this, data)?.emitChildOf(declarationVName)
                }

                val supertypesSymbols = declaration.superTypes.mapNotNull {
                    // Cast should succeed because Classifier = { Class | TypeParameter }, but TypeParameters
                    // can't be a supertype
                    it.classifierOrNull?.owner as IrClass?
                }

                for (superClassSymbol in supertypesSymbols) {
                    val superVName = visitClass(superClassSymbol, data)
                    kotlinEntrySets.emitEdge(declarationVName, EdgeKind.EXTENDS, superVName)
                }
            }

            kotlinEntrySets.leaveScope()
            classVName
        }
    }

    // Overrides are generated in 'visitSimpleFunction'
    override fun visitFunction(declaration: IrFunction, data: Nothing?): Storage.VName? {
        if (declaration.origin == IrDeclarationOrigin.FAKE_OVERRIDE) return null
        return visited.getOrPut(declaration) {
            kotlinEntrySets.enterScope(declaration)

            val functionVName = processDeclaration(declaration) { _ ->
                val body = declaration.body
                when (body) {
                    is IrExpressionBody -> body.accept(this, data)
                    is IrBlockBody -> body.statements.forEach { it.accept(this, data) }
                }
            }

            processFunctionalType(declaration, functionVName)

            kotlinEntrySets.leaveScope()
            functionVName
        }
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Nothing?): Storage.VName? {
        return visited.getOrPut(declaration) {
            val functionVName = visitFunction(declaration, data) ?: return null

            for (overridden in declaration.overriddenSymbols) {
                val overriddenFunctionVName = kotlinEntrySets.getVName(overridden.owner)
                // TODO: subkinds of OVERRIDES?
                kotlinEntrySets.emitEdge(functionVName, EdgeKind.OVERRIDES, overriddenFunctionVName)
            }

            functionVName
        }
    }

    override fun visitProperty(declaration: IrProperty, data: Nothing?): Storage.VName =
        visited.getOrPut(declaration) {
            // NB. Even though IrProperty does have typeParameters, it is actually a quirk in IR hierarchy, and we don't
            // have to enter/leave scope

            val propertyVName = processSimpleTypedDeclaration(declaration, declaration.getter?.returnType!!) {
                declaration.backingField?.accept(this, data)?.emitChildOf(it)
                declaration.getter?.accept(this, data)?.emitChildOf(it)
                declaration.setter?.accept(this, data)?.emitChildOf(it)
            }

            propertyVName
        }

    override fun visitField(declaration: IrField, data: Nothing?): Storage.VName =
        visited.getOrPut(declaration) {
            processDeclaration(declaration) {
                declaration.initializer?.expression?.accept(this, data)
            }
        }

    override fun visitValueParameter(declaration: IrValueParameter, data: Nothing?): Storage.VName =
        visited.getOrPut(declaration) {
            processSimpleTypedDeclaration(declaration, declaration.type) {
                declaration.defaultValue?.expression?.accept(this, data)
            }
        }

    override fun visitVariable(declaration: IrVariable, data: Nothing?): Storage.VName =
        visited.getOrPut(declaration) {
            processSimpleTypedDeclaration(declaration, declaration.type) {
                declaration.initializer?.accept(this, data)
            }
        }


    override fun visitTypeParameter(declaration: IrTypeParameter, data: Nothing?): Storage.VName =
        visited.getOrPut(declaration) {
            val typeParameterVName = processDeclaration(declaration)

            val upperBoundVNames = declaration.superTypes
//                .filter { !KotlinBuiltIns.isNullableAny(it) } // Leave only non-trivial bounds
                .map { visitType(it, declaration.parent as IrTypeParametersContainer) }

            if (upperBoundVNames.isNotEmpty()) {
                kotlinEntrySets.emitOrdinalEdges(typeParameterVName, EdgeKind.BOUNDED_UPPER, upperBoundVNames)
            }

            typeParameterVName
        }

    private fun visitType(irType: IrType, context: IrTypeParametersContainer?): Storage.VName {
        return visitedTypes.getOrPut(irType) {
            kotlinEntrySets.getVNameForTypeReference(irType, context).vNameForWholeType
        }
    }

    // ======== Expressions ========

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Nothing?): Storage.VName? {
        if (expression.operator.isImplicit) return null

        kotlinEntrySets.getReferencedVName(expression.typeOperandClassifier.owner).emitUsage(expression)
        return expression.argument.accept(this, data)
    }

    override fun visitReturn(expression: IrReturn, data: Nothing?): Storage.VName? = expression.value.accept(this, data)

    override fun visitDeclarationReference(expression: IrDeclarationReference, data: Nothing?): Storage.VName =
        kotlinEntrySets.getReferencedVName(expression.symbol.owner).also { it.emitUsage(expression) }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: Nothing?): Storage.VName {
        val owner: IrDeclaration? = expression.symbol.owner
        val referencedVName = kotlinEntrySets.getReferencedVName(expression)
        if (owner?.isExternal() == false) {
            referencedVName.emitUsage(expression)
        }
        return referencedVName
    }

    override fun <T> visitConst(expression: IrConst<T>, data: Nothing?): Storage.VName? = null

    override fun visitCallableReference(expression: IrCallableReference, data: Nothing?): Storage.VName {
        throw UnsupportedOperationException("Callable references are not supported yet")
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: Nothing?): Storage.VName {
        throw UnsupportedOperationException("Local delegated properties are not supported yet")
    }

    override fun visitTypeAlias(declaration: IrTypeAlias, data: Nothing?): Storage.VName {
        throw UnsupportedOperationException("Type Aliases are not supported yet")
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: Nothing?): Storage.VName? = null


    // ======== Utils ==========
    /**
     * Performs generic logic of visiting declaration, including building and emitting of:
     * - node, corresponding to that declaration
     * - ABS-node, if necessary
     * - DEFINES and DEFINES/BINDING anchors for non-external declarations
     *
     * 'processChilds' callback is called for **non-external** declarations after all nodes above are emitted
     *
     * Returns VName of that declaration
     */
    private fun processDeclaration(
        declaration: IrDeclaration,
        processChilds: (Storage.VName) -> Unit = { }
    ): Storage.VName {
        val vName = kotlinEntrySets.getVName(declaration)
        val absVName = if (declaration is IrTypeParametersContainer) processTypeParametersContainer(declaration, vName) else null
        absVName?.let { vName.emitChildOf(it) }

        vName.emitNode(
            declaration.toNodeKind()
        )

        if (!declaration.isExternal()) {
            emitEdgeFromAnchor(declaration, EdgeKind.DEFINES, vName)
            emitDefinesBinding(declaration, absVName ?: vName)

            processChilds(vName)
        }

        return vName
    }

    /**
     * Performs common logic of visiting simple-typed declaration, i.e. declaration,
     * which has the same type as its return type (those are all declarations with type
     * except for function-like declarations -- their type includes type of value parameters)
     *
     * This is essentially 'processDeclaration', which additionally emits 'TYPED' and 'REF' edged
     *
     * Returns VName of that declaration
     */
    private fun processSimpleTypedDeclaration(
        declaration: IrDeclaration,
        returnType: IrType,
        processChilds: (Storage.VName) -> Unit
    ): Storage.VName {
        return processDeclaration(declaration) { declarationVName ->
            val (vNameForWholeType, vNameForReferencedType) = kotlinEntrySets.getVNameForTypeReference(
                returnType, declaration as? IrTypeParametersContainer
            )

            declarationVName.emitTypeOfIfAny(vNameForWholeType)
            declaration.emitRefFromReturnTypeIfAny(vNameForReferencedType)

            processChilds(declarationVName)
        }
    }

    /**
     * Performs common logic of visiting function-like declaration:
     * - emits CHILDOF edges for value parameters
     * - emits REF from return type
     * - builds and emits functional type kythe builtin
     * - emits TYPED edge for current declaration
     */
    private fun processFunctionalType(function: IrFunction, functionVName: Storage.VName) {
        val returnTypeVName = visitType(function.returnType, function).also { function.emitRefFromReturnTypeIfAny(it) }
        val valueParametersTypesVNames: MutableList<Storage.VName> = mutableListOf()
        val valueParametersVNames: MutableList<Storage.VName> = mutableListOf()

        function.extensionReceiverParameter?.let {
            valueParametersVNames += visitValueParameter(it, null)
            valueParametersTypesVNames += visitType(it.type, null)
        }

        for (valueParameter in function.valueParameters) {
            valueParametersVNames += visitValueParameter(valueParameter, null)
            valueParametersTypesVNames += visitType(valueParameter.type, null)
        }

        valueParametersVNames.forEach { it.emitChildOf(functionVName) }

        kotlinEntrySets.emitOrdinalEdges(functionVName, EdgeKind.PARAM, valueParametersVNames, 0)
        val functionTypeEntry = kotlinEntrySets.newFunctionTypeAndEmit(returnTypeVName, valueParametersTypesVNames)
        kotlinEntrySets.emitEdge(functionVName, EdgeKind.TYPED, functionTypeEntry.vName)
    }

    /**
     * Emits:
     * - nodes for type parameters, if any
     * - ABS node for a given declaration if it has type parameters
     *
     * Returns ABS node or null
     */
    private fun processTypeParametersContainer(declaration: IrTypeParametersContainer, declarationVName: Storage.VName): Storage.VName? {
        if (declaration.typeParameters.isEmpty()) return null

        val typeParametersVNames = declaration.typeParameters.map { visitTypeParameter(it, null) }
        return kotlinEntrySets.newAbstractAndEmit(declarationVName, typeParametersVNames, /* markedSource = */ null).vName
    }

    private fun Storage.VName.emitChildOf(parent: Storage.VName) {
        kotlinEntrySets.emitEdge(this, EdgeKind.CHILDOF, parent)
    }

    private fun IrDeclaration.emitRefFromReturnTypeIfAny(referencedTypeVName: Storage.VName?) {
        if (referencedTypeVName == null) return
        val currentFile = this@IrBasedKytheIndexer.currentFile ?: return

        val returnTypeSpan = sourcesManager.spanForReturnTypeIfAny(this, currentFile) ?: return
        val returnTypeAnchor = buildAndEmitAnchor(returnTypeSpan, currentFile)

        kotlinEntrySets.emitEdge(returnTypeAnchor, EdgeKind.REF, referencedTypeVName)
    }

    private fun Storage.VName.emitTypeOfIfAny(type: Storage.VName?) {
        if (type != null) kotlinEntrySets.emitEdge(this, EdgeKind.TYPED, type)
    }

    private fun Storage.VName.emitNode(kind: NodeKind) {
        kotlinEntrySets.newNode(kind, this).build().emit(kotlinEntrySets.emitter)
    }

    private fun Storage.VName.emitUsage(referencingElement: IrElement) {
        val containingFile = this@IrBasedKytheIndexer.currentFile ?: return
        val anchor = buildAndEmitAnchor(sourcesManager.spanForWholeElement(referencingElement, containingFile), containingFile)
        kotlinEntrySets.emitEdge(anchor, EdgeKind.REF, this)
    }

    private fun emitEdgeFromAnchor(declaration: IrDeclaration, kind: EdgeKind, classVName: Storage.VName) {
        val containingFile = this.currentFile ?: return
        val anchor = buildAndEmitAnchor(sourcesManager.spanForWholeElement(declaration, containingFile), containingFile)
        kotlinEntrySets.emitEdge(anchor, kind, classVName)
    }

    private fun emitDefinesBinding(declaration: IrDeclaration, declarationVName: Storage.VName) {
        val containingFile = this.currentFile ?: return
        val anchor = buildAndEmitAnchor(sourcesManager.spanForIdentifierOnly(declaration, containingFile), containingFile)
        kotlinEntrySets.emitEdge(anchor, EdgeKind.DEFINES_BINDING, declarationVName)
    }

    private fun buildAndEmitAnchor(span: Span, containingFile: IrFile): Storage.VName {
        val fileVName: Storage.VName = sourcesManager.fileVName(containingFile)
        return kotlinEntrySets.newAnchorAndEmit(fileVName, span, null).vName
    }
}