/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kythe.signatures

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.types.KotlinType

/**
 * Generates stable unique signatures for Kotlin constructions (declarations, mainly).
 *
 * - "Stable" means that given a fixed compiler version and a fixed declaration, produced
 * signature will be one and the same, regardless of environment configuration.
 * - "Unique" means that given a fixed compiler version, compilation input and two declarations
 * from that input, their signatures are equal iff this is one and the same declaration.
 *
 * NB. This is WIP, see 'KotlinSignaturesProvider' for details
 */
interface SignaturesProvider {
    /**
     * Gets "full" (i.e. with TAG) signature of a given element/type.
     */
    fun getFullSignature(irElement: IrElement, immediateContext: IrTypeParametersContainer? = null): String

    fun getFullSignatureOfType(irType: IrType, immediateContext: IrTypeParametersContainer?): String

    /**
     * Following two methods should be called when entering/leaving scope of any type parameters container
     * This is needed for generics to be resolved correctly
     */
    fun enterScope(typeParametersContainer: IrTypeParametersContainer)

    fun leaveScope()
}

