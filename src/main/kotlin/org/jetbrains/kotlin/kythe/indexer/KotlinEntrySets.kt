/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kythe.indexer

import com.google.devtools.kythe.analyzers.base.*
import com.google.devtools.kythe.platform.shared.StatisticsCollector
import com.google.devtools.kythe.proto.Analysis
import com.google.devtools.kythe.proto.Storage
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSymbolDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.kythe.signatures.SignaturesProvider
import org.jetbrains.kotlin.types.ErrorType
import java.lang.UnsupportedOperationException

class KotlinEntrySets(
    statisticsCollector: StatisticsCollector,
    emitter: FactEmitter,
    compilationVName: Storage.VName,
    requiredInputs: MutableList<Analysis.CompilationUnit.FileInput>,
    private val signaturesProvider: SignaturesProvider
) : KytheEntrySets(statisticsCollector, emitter, compilationVName, requiredInputs) {
    private val absNodesCache: HashMap<Storage.VName, EntrySet> = hashMapOf()
    private val corpus: String = ""
    private val root: String = ""
    private val path: String = ""
    private val language: String = "kotlin"

    fun getVName(irElement: IrElement): Storage.VName {
        return signaturesProvider.getFullSignature(irElement, (irElement as? IrDeclaration)?.parent as? IrTypeParametersContainer).toVName()
    }

    // Conceptual difference from `getVName` is that this method keeps kythe referencing
    // semantic, e.g. for Generic class it will return VName for 'abs' node (not for 'record/class' node)
    fun getReferencedVName(irElement: IrElement): Storage.VName {
        val owner = when (irElement) {
            is IrDeclarationReference -> irElement.symbol.owner
            is IrSymbolOwner -> irElement.symbol.owner
            else -> throw IllegalStateException("Don't know how to resolve reference target for $irElement")
        }
        return getVName(owner)
    }

    fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {
        signaturesProvider.enterScope(irTypeParametersContainer)
    }

    fun leaveScope() {
        signaturesProvider.leaveScope()
    }

    // vNameForWholeType -- vname for e.g. TYPEOF edge (i.e. vName of the whole type, with type arguments taken into consideration)
    // vNameForReferencing -- vname for e.g. REF edge (i.e. vName of the corresponding unsubstituted type)
    // For non-generic types, vNameForWholeType === vNameForReferencing
    data class TypeReferenceVNames(val vNameForWholeType: Storage.VName, val vNameForReferencing: Storage.VName)

    fun getVNameForTypeReference(irType: IrType, immediateContext: IrTypeParametersContainer?): TypeReferenceVNames {
        return when {
            irType is IrSimpleType && irType.arguments.isNotEmpty() -> getVNamesForGenericType(irType, immediateContext)

            irType is IrSimpleType && irType.arguments.isEmpty() -> {
                val node = NodeBuilder(
                    irType.toNodeKind(),
                    signaturesProvider.getFullSignatureOfType(irType, immediateContext).toVName()
                ).build()

                node.emit(emitter)

                // Non-generic type: vnames for whole type and for referenced type are the same
                TypeReferenceVNames(node.vName, node.vName)
            }

            irType is ErrorType -> throw UnsupportedOperationException("Error types are not supported yet")

            irType is IrDynamicType -> throw UnsupportedOperationException("Dynamic types are not supported yet")

            else -> throw IllegalStateException("Unknown irType: $irType of type ${irType::class}")
        }
    }

    private fun getVNamesForGenericType(irType: IrSimpleType, immediateContext: IrTypeParametersContainer?): TypeReferenceVNames {
        // If 'irType' is generic like 'Foo<String, Int, Bar<Int, Int?>>', then 'unsubstitutedType' is Foo<T, Q, R>
        val unsubstitutedElement = irType.classifier.owner
        val vNameForUnsubstitutedElement =
            signaturesProvider.getFullSignature(unsubstitutedElement, unsubstitutedElement as IrTypeParametersContainer).toVName()

        val absNode = absNodesCache.getOrPut(vNameForUnsubstitutedElement) {
            val vNameForTypeParameters = (unsubstitutedElement as IrTypeParametersContainer).typeParameters.map {
                signaturesProvider.getFullSignature(it, immediateContext).toVName()
            }

            newAbstractAndEmit(vNameForUnsubstitutedElement, vNameForTypeParameters, null).also { it.emit(emitter) }
        }

        val argumentsVNames = irType.arguments.map {
            when (it) {
                is IrStarProjection -> throw UnsupportedOperationException("Star projections are not supported")
                is IrTypeProjection -> getVNameForTypeReference(it.type, immediateContext).vNameForReferencing // TODO: variance?
                else -> throw IllegalStateException("Unknown IrTypeArgument: $it of type ${it::class}")
            }
        }
        val vNameForWholeType = newTApplyAndEmit(absNode.vName, argumentsVNames)

        return TypeReferenceVNames(
            vNameForWholeType = vNameForWholeType.vName,
            vNameForReferencing = absNode.vName
        )
    }

    private fun String.toVName(): Storage.VName {
        return Storage.VName.newBuilder()
            .setSignature(this)
            .setCorpus(corpus)
            .setRoot(root)
            .setPath(path)
            .setLanguage(language)
            .build()
    }

    private fun IrType.toNodeKind(): NodeKind = when (this) {
        is IrSimpleType -> (classifier.owner as IrSymbolDeclaration<*>).toNodeKind()
        is IrErrorType -> throw UnsupportedOperationException("Error types are not supported yet")
        is IrDynamicType -> throw UnsupportedOperationException("Dynamic types are not supported yet")
        else -> throw IllegalStateException("Unknown IrType $this of type ${this::class}")
    }
}

