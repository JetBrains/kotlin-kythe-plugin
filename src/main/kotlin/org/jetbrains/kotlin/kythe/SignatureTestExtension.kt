package org.jetbrains.kotlin.kythe

import com.google.devtools.kythe.extractors.shared.FileVNames
import com.google.devtools.kythe.platform.shared.StatisticsCollector
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.kythe.signatures.KotlinSignaturesProvider
import org.jetbrains.kotlin.kythe.signatures.SignaturesProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.utils.Printer
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter

typealias IrElementsFilter = (IrElement) -> Boolean

class SignatureTestExtension : AnalysisHandlerExtension {
    override fun analysisCompleted(project: Project, module: ModuleDescriptor, bindingTrace: BindingTrace, files: Collection<KtFile>): AnalysisResult? {
        val psi2ir = Psi2IrTranslator(LanguageVersionSettingsImpl.DEFAULT, Psi2IrConfiguration(ignoreErrors = true))
        val generatorContext = psi2ir.createGeneratorContext(module, bindingTrace.bindingContext)
        val ir = psi2ir.generateModuleFragment(generatorContext, files)

        val file = files.single()
        doTest(ir, File(file.virtualFilePath), generatorContext)

        return AnalysisResult.success(bindingTrace.bindingContext, module, shouldGenerateCode = false)
    }

    private fun doTest(root: IrModuleFragment, testFile: File, generatorContext: GeneratorContext) {
        val elementsFilter = parseFilterFromFileText(testFile)
        val dir = File(testFile.parent)
        val actualFile = createExpectedTextFile(testFile, dir, testFile.name.replace(".kt", ".txt.new"))
        val actualText = dumpAllSignatures(root, elementsFilter, generatorContext)
        actualFile.writeText(actualText)
    }

    private fun createExpectedTextFile(testFile: File, dir: File, fileName: String): File {
        val textFile = File(dir, fileName)
        if (!textFile.exists()) {
            assert(textFile.createNewFile()) { "Can't create an expected text containingFile: ${textFile.absolutePath}" }
            PrintWriter(FileWriter(textFile)).use {
                it.println("$fileName: new expected text containingFile for ${testFile.name}")
            }
        }
        return textFile
    }

    private fun parseFilterFromFileText(file: File): IrElementsFilter {
        val firstLine = file.readText().lines().first()
        if (!firstLine.startsWith(EXCLUDE_DIRECTIVE)) return RENDER_ALL

        val excludedElements = firstLine.removePrefix(EXCLUDE_DIRECTIVE).split(",").map { it.trim() }
        return { irElement ->
            val renderedElement = irElement.render()
            excludedElements.all { it !in renderedElement }
        }
    }

    private fun dumpAllSignatures(
            root: IrElement,
            elementsFilter: IrElementsFilter,
            generatorContext: GeneratorContext
    ): String {
        val collector =
                SignaturesCollector(
                        KotlinSignaturesProvider(generatorContext.symbolTable),
                        elementsFilter
                )

        return buildString {
            root.accept(collector, Printer(this))
        }
    }

    private class SignaturesCollector(
            private val signaturesProvider: SignaturesProvider,
            private val elementsFilter: IrElementsFilter
    ) : IrElementVisitor<Unit, Printer> {

        override fun visitElement(element: IrElement, data: Printer) {
            // skip
        }

        override fun visitFile(declaration: IrFile, data: Printer) {
            declaration.acceptChildren(this, data)
        }

        override fun visitModuleFragment(declaration: IrModuleFragment, data: Printer) {
            declaration.acceptChildren(this, data)
        }

        override fun visitPackageFragment(declaration: IrPackageFragment, data: Printer) {
            data.println(declaration.render())
            data.println(signaturesProvider.getFullSignature(declaration))

            data.pushIndent()
            declaration.acceptChildren(this, data)
            data.popIndent()
        }

        override fun visitBody(body: IrBody, data: Printer) {
            body.acceptChildren(this, data)
        }


        override fun visitDeclaration(declaration: IrDeclaration, data: Printer) {
            if (declaration is IrTypeParametersContainer) signaturesProvider.enterScope(declaration)

            if (!elementsFilter(declaration)) return

            data.println(declaration.render())
            if (declaration !is IrAnonymousInitializer) { // No signatures for anonymous initializers
                data.println(signaturesProvider.getFullSignature(declaration))
            }

            data.pushIndent()
            declaration.acceptChildren(this, data)
            data.println()
            data.popIndent()

            if (declaration is IrTypeParametersContainer) signaturesProvider.leaveScope()
        }
    }

    companion object {
        const val EXCLUDE_DIRECTIVE: String = "// !EXCLUDE: "

        val RENDER_ALL: IrElementsFilter = { true }
    }
}