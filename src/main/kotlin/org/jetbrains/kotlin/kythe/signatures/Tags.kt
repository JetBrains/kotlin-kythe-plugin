/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kythe.signatures

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType

enum class Tag(val id: String) {
    PACKAGE_FRAGMENT("PACK"),

    CLASS("CLASS"),
    INTERFACE("INTERFACE"),
    OBJECT("OBJ"),
    COMPANION_OBJECT("COMP"),
    ENUM_CLASS("ENUM"),
    ENUM_ENTRY("ENUM_ENTRY"),
    TYPE_ALIAS("TALIAS"),

    FUNCTION("FUN"),
    CONSTRUCTOR("CTOR"),
    PROPERTY("PROP"),
    BACKING_FIELD("BFIELD"),
    GETTER("GET"),
    SETTER("SET"),

    TYPE_PARAMETER("TPARAM"),
    VALUE_PARAMETER("VPARAM"),
    LOCAL_PROPERTY("LPROP"), // TODO: drop from signatures?
    VARIABLE("VAR"), // TODO: I think we have either merge those two or distiguish them clearly

}

internal val IrType.tag: Tag
    get() = when (this) {
        is IrDynamicType -> throw UnsupportedOperationException("Dynamics are not supported yet")

        is IrErrorType -> throw UnsupportedOperationException("Error types are not supported yet")

        is IrSimpleType -> classifier.owner.tag

        else -> throw IllegalStateException("Unknown IrType $this of type ${this::class}")
    }

internal val IrElement.tag: Tag
    get() = when (this) {
        is IrPackageFragment -> Tag.PACKAGE_FRAGMENT
        is IrClass -> kind.tag(isCompanion)
        is IrDeclaration -> when (this) {
            is IrEnumEntry -> Tag.ENUM_ENTRY
            is IrSimpleFunction -> Tag.FUNCTION
            is IrConstructor -> Tag.CONSTRUCTOR
            is IrProperty -> Tag.PROPERTY
            is IrField -> Tag.BACKING_FIELD
            is IrVariable -> Tag.VARIABLE
            is IrLocalDelegatedProperty -> Tag.LOCAL_PROPERTY
            is IrTypeAlias -> throw IllegalStateException("Type aliases are not supported yet")
            is IrTypeParameter -> Tag.TYPE_PARAMETER
            is IrValueParameter -> Tag.VALUE_PARAMETER
            else -> throw IllegalStateException("Don't know how to generate tag for $this")
        }
        else -> throw IllegalStateException("Don't know how to get Tag for $this")
    }

internal fun ClassKind.tag(isCompanionObject: Boolean): Tag =
    when (this) {
        ClassKind.CLASS -> if (isCompanionObject) Tag.COMPANION_OBJECT else Tag.CLASS
        ClassKind.INTERFACE -> Tag.INTERFACE
        ClassKind.ENUM_CLASS -> Tag.ENUM_CLASS
        ClassKind.ENUM_ENTRY -> Tag.ENUM_ENTRY
        ClassKind.ANNOTATION_CLASS -> throw IllegalStateException("Annotations are not supported yet")
        ClassKind.OBJECT -> Tag.OBJECT

    }
