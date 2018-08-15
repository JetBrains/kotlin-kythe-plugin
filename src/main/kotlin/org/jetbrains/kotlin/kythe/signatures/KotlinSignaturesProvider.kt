/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kythe.signatures

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.ScopedTypeParametersResolver
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.kythe.indexer.TypeResolver
import org.jetbrains.kotlin.kythe.indexer.TypeResolverImpl
import org.jetbrains.kotlin.types.Variance

/**
 * This is a prototype-implementation of a generator of stable identifiers for
 * kotlin constructions.
 *
 * Stability guarantees:
 * Generated signatures are guaranteed to be stable for one and the same
 * compilation input, as per [SignaturesProvider] javadoc.
 *
 * See examples of generated signatures in 'testData/signatures'
 */
class KotlinSignaturesProvider(symbolTable: SymbolTable) : SignaturesProvider {
    private val elementSignatureGenerator = SingleElementSignatureGenerator()
    private val typeResolver: TypeResolver = TypeResolverImpl(ScopedTypeParametersResolver(), symbolTable)

    override fun enterScope(typeParametersContainer: IrTypeParametersContainer) {
        typeResolver.enterScope(typeParametersContainer)
    }

    override fun leaveScope() {
        typeResolver.exitScope()
    }

    /**
     * Returns full signature (i.e. with tag) of a given element
     * Also adds type of that irElement, if it has one, separated by the semicolon (';')
     */
    override fun getFullSignature(irElement: IrElement, immediateContext: IrTypeParametersContainer?): String =
        irElement.tag.id + ":" + elementSignature(irElement) + irElement.returnTypeSignatureIfAny()

    override fun getFullSignatureOfType(irType: IrType, immediateContext: IrTypeParametersContainer?): String =
        irType.tag.id + ":" + typeSignature(irType, immediateContext)


    private fun elementSignature(irElement: IrElement, typeArguments: List<IrTypeArgument>? = null): String {
        return buildString {
            val pathFromRoot = irElement.parentsWithoutMe().asReversed()
            for (pathElement in pathFromRoot) {
                writeSeparator(pathElement)
                writeSingleElementSignature(pathElement, typeArguments = null)
            }

            // We have to hand-unroll last iteration, because given element may have type arguments, which has to be substituted
            // instead of a type parameters
            writeSeparator(irElement)
            writeSingleElementSignature(irElement, typeArguments)
        }
    }

    private fun typeSignature(type: IrType, immediateContext: IrTypeParametersContainer?): String = when (type) {
        is IrErrorType -> "<ERROR_TYPE>"

        is IrDynamicType -> throw UnsupportedOperationException("Dynamic types are not supported yet")

        is IrSimpleType -> {
            val correspondingElement = type.classifier.owner
            val elementSignature =
                if (correspondingElement is IrTypeParameter && correspondingElement in immediateContext?.typeParameters.orEmpty()) {
                    // I.e. we're looking for signature for type of 'x' in the declaration 'fun <T> foo(x: T)'
                    // In such cases we render TypeParameter as just 'T', without prepending parent's signature,
                    // because it would lead to infinite recursion.
                    correspondingElement.name.asString()
                } else {
                    /*
                    Here we have a small quirk of types representation. Consider following case:

                      class Outer<T> {
                        inner class Inner<Q>
                      }

                    In this example, classifier for Inner has 1 type parameter, namely, 'Q'.
                    However, type for Inner (i.e. return type of implicit default constructor of Inner<Q>)
                    has *two* type arguments: "T" and "Q". This is because for Kotlin compiler it is convenient
                    to treat all "inner" types as capturing outer generics.

                    If this is undesired, one can uncomment next line, which will take type arguments which
                    exactly correspond to type parameters of the element in subject:
                    */
                    // val typeArguments = type.arguments.takeLast((correspondingElement as? IrTypeParametersContainer)?.typeParameters?.size ?: 0)
                    val typeArguments = type.arguments
                    elementSignature(correspondingElement, typeArguments)
                }

            elementSignature + if (type.hasQuestionMark) "?" else ""
        }

        else -> throw IllegalStateException("Unknown IrType $type, of type ${type::class}")
    }

    private fun StringBuilder.writeSingleElementSignature(irElement: IrElement, typeArguments: List<IrTypeArgument>?) {
        // Minor prettification: render type parameters before function
        // For everything else they are rendered after element signature, but for function signature includes
        // list of parameters, and diamond after it look ugly
        if (irElement is IrFunction && typeArguments == null) writeTypeParameters(irElement)

        irElement.accept(elementSignatureGenerator, this)

        if (irElement !is IrFunction && typeArguments == null)
            writeTypeParameters(irElement)
        else
            writeTypeArguments(typeArguments, irElement as? IrTypeParametersContainer)
    }

    private fun StringBuilder.writeSeparator(irElement: IrElement) {
        if (this.isEmpty()) return // Do not write separator before first element

        val separator = when (irElement) {
            is IrClass -> if (irElement.isInner) "$" else "."

            is IrValueParameter, is IrField -> "#"

            is IrTypeParameter -> "~"

            else -> "."
        }

        append(separator)
    }

    private fun StringBuilder.writeTypeParameters(irElement: IrElement) {
        if (irElement is IrTypeParametersContainer && irElement.typeParameters.isNotEmpty()) {
            append(irElement.typeParameters.joinToString(prefix = "<", postfix = ">") { typeParameterSignature(it) })
        }
    }

    private fun StringBuilder.writeTypeArguments(typeArguments: List<IrTypeArgument>?, immediateContext: IrTypeParametersContainer?) {
        if (typeArguments == null || typeArguments.isEmpty()) return
        append(typeArguments.joinToString(separator = ",", prefix = "<", postfix = ">") {
            typeArgumentSignature(it, immediateContext)
        })
    }

    private fun typeArgumentSignature(typeArgument: IrTypeArgument, immediateContext: IrTypeParametersContainer?): String =
        when (typeArgument) {
            is IrStarProjection -> "*"

            is IrTypeProjection -> {
                val varianceString = if (typeArgument.variance != Variance.INVARIANT) typeArgument.variance.label + " " else ""
                val typeSignature = typeSignature(typeArgument.type, immediateContext)

                varianceString + typeSignature
            }

            else -> throw IllegalStateException("Unknown type argument: $typeArgument of type ${typeArgument::class}")
        }

    private fun typeParameterSignature(irTypeParameter: IrTypeParameter): String {
        return buildString {
            if (irTypeParameter.variance != Variance.INVARIANT) {
                append(irTypeParameter.variance.label + " ")
            }

            append(irTypeParameter.name)
        }
    }

    private fun IrElement.returnTypeSignatureIfAny(): String {
        val returnType = when (this) {
            is IrFunction -> returnType
            is IrProperty -> this.getter!!.returnType
            is IrValueParameter -> type
            else -> return ""
        }

        return ";" + typeSignature(returnType, this as? IrTypeParametersContainer)
    }

    private fun List<IrValueParameter>.joinValueParametersTypes(owner: IrElement): String =
        joinToString(prefix = "(", postfix = ")") {
            typeSignature(it.type, owner as? IrTypeParametersContainer)
        }

    // Writes signature of a given element in passed 'StringBuilder'
    private inner class SingleElementSignatureGenerator : IrElementVisitor<Unit, StringBuilder> {

        override fun visitElement(element: IrElement, data: StringBuilder) {
            throw IllegalStateException("Don't know how to generate signature for $element")
        }

        override fun visitPackageFragment(declaration: IrPackageFragment, data: StringBuilder) {
            data.append(declaration.fqName.asString())
        }

        override fun visitClass(declaration: IrClass, data: StringBuilder) {
            data.append(declaration.name)
        }

        override fun visitTypeAlias(declaration: IrTypeAlias, data: StringBuilder) {
            throw IllegalStateException("Type aliases are not supported yet")
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction, data: StringBuilder) {
            val valueParameters = declaration.valueParameters.joinValueParametersTypes(declaration)

            data.append(declaration.name).append(valueParameters)
        }

        override fun visitConstructor(declaration: IrConstructor, data: StringBuilder) {
            val symbol = typeResolver.resolveToSymbol(declaration.returnType)
            val ownerClassName = when (symbol) {
                is IrClassSymbol -> symbol.owner.name
                is IrTypeParameterSymbol -> symbol.owner.name
                is IrEnumEntrySymbol -> symbol.owner.name
                else -> throw IllegalStateException("Unrecognized IrClassifierSymbol: $symbol")
            }

            data.append(ownerClassName)
                .append(declaration.valueParameters.joinValueParametersTypes(declaration))
        }

        override fun visitProperty(declaration: IrProperty, data: StringBuilder) {
            data.append(declaration.name)
        }

        override fun visitVariable(declaration: IrVariable, data: StringBuilder) {
            data.append(declaration.name)
        }

        override fun visitField(declaration: IrField, data: StringBuilder) {
            data.append("field")
        }

        override fun visitTypeParameter(declaration: IrTypeParameter, data: StringBuilder) {
            data.append(declaration.name)
        }

        override fun visitValueParameter(declaration: IrValueParameter, data: StringBuilder) {
            data.append(declaration.name)
        }

        override fun visitEnumEntry(declaration: IrEnumEntry, data: StringBuilder) {
            data.append(declaration.name)
        }
    }
}