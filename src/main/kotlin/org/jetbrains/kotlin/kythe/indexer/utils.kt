/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kythe.indexer

import com.google.common.hash.Hashing
import com.google.devtools.kythe.analyzers.base.NodeKind
import com.google.devtools.kythe.extractors.shared.ExtractorUtils
import com.google.devtools.kythe.extractors.shared.FileVNames
import com.google.devtools.kythe.proto.Analysis
import com.google.devtools.kythe.proto.Storage
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrErrorExpression
import org.jetbrains.kotlin.kythe.KytheIndexerComponentRegistrar.Companion.DEFAULT_CORPUS
import org.jetbrains.kotlin.psi.KtFile
import java.lang.UnsupportedOperationException

fun getEnvironment(value: String): String? = System.getProperty(value) ?: System.getenv(value)

fun getRequiredInputs(
    inputFiles: Collection<KtFile>,
    fileVNames: FileVNames = FileVNames.staticCorpus(DEFAULT_CORPUS)
): MutableList<Analysis.CompilationUnit.FileInput> {
    val fileDatas = ExtractorUtils.convertBytesToFileDatas(inputFiles.map { it.virtualFilePath to it.text.toByteArray() }.toMap())
    // TODO: relativize properly
    return ExtractorUtils.toFileInputs(fileVNames, { it }, fileDatas)
}

fun getCompilationVName(files: Collection<KtFile>, target: String? = null): Storage.VName {
    return Storage.VName.newBuilder()
        .setSignature(target ?: createTargetFromFiles(files))
        .setLanguage("kotlin").build()
}

fun createTargetFromFiles(files: Collection<KtFile>): String {
    // Following logic from Java indexer
    return "#" + Hashing.sha256().hashUnencodedChars(files.joinToString(":"))
}

fun IrDeclaration.isExternal(): Boolean =
    origin == IrDeclarationOrigin.IR_BUILTINS_STUB || origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB || startOffset == -1

fun IrDeclaration.toNodeKind(): NodeKind {
    return when (this) {
        is IrModuleFragment -> throw IllegalStateException("Modules don't have a corresponding Kythe node (yet?)")
        is IrFile -> NodeKind.FILE
        is IrClass -> kind.toNodeKind(isCompanion)
        is IrEnumEntry -> NodeKind.CONSTANT
        is IrSimpleFunction -> NodeKind.FUNCTION
        is IrConstructor -> NodeKind.FUNCTION_CONSTRUCTOR
        is IrProperty -> NodeKind.PROPERTY
        is IrField -> NodeKind.VARIABLE_FIELD
        is IrVariable -> NodeKind.VARIABLE_LOCAL
        is IrLocalDelegatedProperty -> throw UnsupportedOperationException("Delegation isn't supported yet")
        is IrTypeAlias -> NodeKind.TALIAS
        is IrTypeParameter -> NodeKind.ABS_VAR
        is IrValueParameter -> NodeKind.VARIABLE_PARAMETER
        is IrErrorDeclaration, is IrErrorExpression -> throw UnsupportedOperationException("Errors are not supported yet")
        else -> throw IllegalStateException("Don't know how to translate $this to kythe NodeKind")
    }
}

fun ClassKind.toNodeKind(isCompanion: Boolean): NodeKind = when (this) {
    ClassKind.CLASS -> NodeKind.RECORD_CLASS
    ClassKind.INTERFACE -> NodeKind.INTERFACE
    ClassKind.ENUM_CLASS -> NodeKind.SUM_ENUM_CLASS
    ClassKind.ENUM_ENTRY -> NodeKind.CONSTANT
    ClassKind.ANNOTATION_CLASS -> throw UnsupportedOperationException("Annotations are not supported yet")
    ClassKind.OBJECT -> if (isCompanion) NodeKind.COMPANION else NodeKind.OBJECT
}
