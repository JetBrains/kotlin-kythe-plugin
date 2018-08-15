/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kythe.signatures

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType

internal fun IrElement.parentsWithoutMe(): List<IrElement> =
    generateSequence(this.nextParent) { it.nextParent }.toList()

internal fun IrElement.parentsWithMe(): List<IrElement> =
    generateSequence(this) { it.nextParent }.toList()

internal val IrElement.nextParent: IrElement?
    get() = when (this) {
        // Workaround quirk of IR: parent of property accessors is class instead of a property
        is IrFunction -> if (isAccessor()) getParentPropertyOfAccessor() else parent as? IrElement
        is IrField -> getParentPropertyOfBackingField()
        is IrDeclaration -> parent as? IrElement
        else -> null
    }

fun IrFunction.isAccessor(): Boolean = this.descriptor is PropertyAccessorDescriptor

fun IrFunction.getParentPropertyOfAccessor(): IrElement? {
    // XXX: hack and create IrProperty ad hoc, because it is not a symbol in IR, and symbol table doesn't know about IrProperties
    val correspondingPropertyDescriptor = (this.descriptor as? PropertyAccessorDescriptor)?.correspondingProperty ?: return null
    return IrPropertyImpl(
        startOffset = -1,
        endOffset = -1,
        origin = IrDeclarationOrigin.DEFINED,
        isDelegated = correspondingPropertyDescriptor.isDelegated,
        descriptor = correspondingPropertyDescriptor
    ).also { property -> property.parent = this.parent }
}

fun IrField.getParentPropertyOfBackingField(): IrElement? {
    val correspondingPropertyDescriptor = this.descriptor
    return IrPropertyImpl(
        startOffset = -1,
        endOffset = -1,
        origin = IrDeclarationOrigin.DEFINED,
        isDelegated = correspondingPropertyDescriptor.isDelegated,
        descriptor = correspondingPropertyDescriptor
    ).also { property -> property.parent = this.parent }
}

fun IrTypeParameter.nonTrivialSupertypes(): List<IrType> =
    this.superTypes.filter { !it.isNullableAny() }

fun IrType.isNullableAny(): Boolean {
    if (this !is IrSimpleType || !this.hasQuestionMark) return false

    val classifier = this.classifier.owner as? IrClass ?: return false
    val containingPackageFragment = classifier.parent as? IrExternalPackageFragment ?: return false

    return classifier.name == KotlinBuiltIns.FQ_NAMES.any.shortName() &&
            containingPackageFragment.fqName == KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME
}

val IrTypeOperator.isImplicit: Boolean
    get() = when (this) {
        IrTypeOperator.IMPLICIT_CAST, IrTypeOperator.IMPLICIT_NOTNULL,
        IrTypeOperator.IMPLICIT_COERCION_TO_UNIT, IrTypeOperator.IMPLICIT_INTEGER_COERCION -> true

        else -> false
    }