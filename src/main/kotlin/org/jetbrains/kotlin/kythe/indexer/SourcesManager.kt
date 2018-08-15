/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kythe.indexer

import com.google.devtools.kythe.proto.Storage
import com.google.devtools.kythe.util.Span
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.psi2ir.PsiSourceManager
import java.io.File
import java.nio.charset.Charset

interface SourcesManager {
    fun fileVName(file: IrFile): Storage.VName
    fun fileContent(file: IrFile): ByteArray
    fun fileCharset(file: IrFile): Charset

    fun spanForIdentifierOnly(declaration: IrDeclaration, currentFile: IrFile): Span
    fun spanForWholeElement(element: IrElement, currentFile: IrFile): Span

    // Returns null in case declaration has no explicit return type
    fun spanForReturnTypeIfAny(irDeclaration: IrDeclaration, currentFile: IrFile): Span?
}

class PsiBasedSourcesManager(
    private val sourceManager: PsiSourceManager,
    private val corpus: String,
    private val root: String
) : SourcesManager {
    override fun fileVName(file: IrFile): Storage.VName {
        /*
         We have to do some hoops here because generally we pass 'root' as
         relative to something (otherwise testdata won't be portable), like
         "testdata/indexer/multifile".

         On the other hand, IrFile.fileEntry.name returns absolute path,
         so we have to carefully absolutize root, and then relativize file wrt
         root.
         */
        val absoluteFile = File(file.fileEntry.name)
        val absoluteRoot = File(root).absoluteFile
        val relativePath = absoluteFile.relativeTo(absoluteRoot).path
        return Storage.VName.newBuilder()
            .setPath(relativePath)
            .setCorpus(corpus)
            .setRoot(root)
            .build()
    }

    /*
     TODO: handle different charsets properly

     Kotlin model works with character-offsets, while kythe model required byte-offsets.
     In general case, converting char-offsets to byte-offsets is a complex task, so currently
     we're using temporary hack: we're exposing all files only as US-ASCII-encoded, which allows
     us to easily convert char-offsets to byte-offsets.
    */
    override fun fileContent(file: IrFile): ByteArray {
        val byteBuffer = Charsets.US_ASCII.encode(sourceManager.getKtFile(file)?.text ?: "")

        return ByteArray(byteBuffer.remaining()).apply {
            byteBuffer.get(this)
        }
    }

    override fun fileCharset(file: IrFile): Charset = Charsets.US_ASCII

    override fun spanForIdentifierOnly(declaration: IrDeclaration, currentFile: IrFile): Span {
        if (declaration is IrValueParameter && declaration.index == -1) { // receiver of extension function
            return Span(declaration.startOffset, declaration.endOffset)
        }

        val correspondingPsiDeclaration = declaration.findCorrespondingPsiDeclaration(currentFile)

        val textRange: TextRange = when (correspondingPsiDeclaration) {
            // it is permittable for companion object to not have nameIdentifier
            is KtObjectDeclaration -> correspondingPsiDeclaration.nameIdentifier?.textRangeWithoutComments
                ?: correspondingPsiDeclaration.getObjectKeyword()!!.textRangeWithoutComments

            is KtNamedDeclaration -> correspondingPsiDeclaration.nameIdentifier?.textRangeWithoutComments!!

            is KtConstructor<*> -> correspondingPsiDeclaration.textRangeWithoutComments

            is KtPropertyAccessor -> correspondingPsiDeclaration.namePlaceholder.textRangeWithoutComments

            else -> throw IllegalStateException("Don't know how to get named PsiElement for declaration $correspondingPsiDeclaration, ${correspondingPsiDeclaration.text}")
        }

        return textRange.toSpan()
    }

    private fun IrDeclaration.findCorrespondingPsiDeclaration(currentFile: IrFile): KtDeclaration {
        val containingKtFile = sourceManager.getKtFile(currentFile)
            ?: throw IllegalStateException("Can't find KtFile for $currentFile")

        val leafPsiElement = containingKtFile.viewProvider.findElementAt(startOffset)
            ?: throw IllegalStateException("Can't find PsiElement for ${this}")

        return leafPsiElement.getNonStrictParentOfType()
            ?: throw IllegalStateException("Can't find parent PsiDeclaration for ${this}")
    }

    override fun spanForReturnTypeIfAny(irDeclaration: IrDeclaration, currentFile: IrFile): Span? {
        if (irDeclaration is IrConstructor || irDeclaration.descriptor is PropertyAccessorDescriptor) return null

        val correspondingPsiDeclaration = irDeclaration.findCorrespondingPsiDeclaration(currentFile)
        val textRange = when {
            correspondingPsiDeclaration is KtCallableDeclaration -> correspondingPsiDeclaration.typeReference?.getTextRangeForTypeReference()
                ?: correspondingPsiDeclaration.textRangeWithoutComments

            else -> throw IllegalStateException(
                "Don't know how to get type reference for declaration $correspondingPsiDeclaration, " +
                        "${correspondingPsiDeclaration.text} from $irDeclaration"
            )
        }

        return textRange.toSpan()
    }

    override fun spanForWholeElement(element: IrElement, currentFile: IrFile): Span = Span(element.startOffset, element.endOffset)

    private fun TextRange.toSpan(): Span = Span(startOffset, endOffset)

    private fun KtTypeReference.getTextRangeForTypeReference(): TextRange? {
        val typeElement = typeElement ?: return null
        val specificTextRange = when (typeElement) {
            is KtNullableType -> typeElement.innerType?.textRangeWithoutComments
            is KtUserType -> typeElement.referenceExpression?.textRangeWithoutComments
            else -> null
        }

        return specificTextRange ?: typeElement.textRangeWithoutComments
    }

}