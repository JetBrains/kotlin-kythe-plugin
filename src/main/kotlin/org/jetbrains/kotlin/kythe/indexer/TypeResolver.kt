/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kythe.indexer

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.ScopedTypeParametersResolver
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.referenceClassifier
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.types.KotlinType

interface TypeResolver {
    fun resolveToSymbol(type: KotlinType): IrSymbol?
    fun resolveToSymbol(descriptor: DeclarationDescriptor): IrSymbol?
    fun resolveToSymbol(irType: IrType): IrSymbol?

    fun enterScope(typeParametersContainer: IrTypeParametersContainer)
    fun exitScope()
}

class TypeResolverImpl(
    private val typeParametersResolver: ScopedTypeParametersResolver,
    private val symbolTable: SymbolTable
) : TypeResolver {
    override fun resolveToSymbol(type: KotlinType): IrSymbol? {
        val classifier = type.constructor.declarationDescriptor ?: return null
        return resolveToSymbol(classifier)
    }

    override fun resolveToSymbol(descriptor: DeclarationDescriptor): IrSymbol? {
        return when {
            descriptor is TypeParameterDescriptor -> typeParametersResolver.resolveScopedTypeParameter(descriptor)
            descriptor is ClassDescriptor && descriptor.kind == ClassKind.ENUM_ENTRY -> symbolTable.referenceEnumEntry(descriptor)
            descriptor is ClassifierDescriptor -> symbolTable.referenceClassifier(descriptor)
            descriptor is PropertyDescriptor -> symbolTable.referenceField(descriptor)
            descriptor is CallableDescriptor -> symbolTable.referenceFunction(descriptor) // constructor or function
            descriptor is ValueDescriptor -> symbolTable.referenceValue(descriptor) // variable or parameter
            else -> throw IllegalStateException("Unknown descriptor: $descriptor")
        }
    }

    override fun resolveToSymbol(irType: IrType): IrSymbol? = irType.classifierOrNull?.owner?.symbol

    override fun enterScope(typeParametersContainer: IrTypeParametersContainer) {
        typeParametersResolver.enterTypeParameterScope(typeParametersContainer)
    }

    override fun exitScope() {
        typeParametersResolver.leaveTypeParameterScope()
    }
}