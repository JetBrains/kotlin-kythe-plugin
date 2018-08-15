/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kythe

import com.google.devtools.kythe.analyzers.base.StreamFactEmitter
import com.google.devtools.kythe.extractors.shared.FileVNames
import com.google.devtools.kythe.platform.shared.StatisticsCollector
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.kythe.indexer.IrBasedKytheIndexer
import org.jetbrains.kotlin.kythe.indexer.PsiBasedSourcesManager
import org.jetbrains.kotlin.kythe.indexer.getCompilationVName
import org.jetbrains.kotlin.kythe.indexer.getRequiredInputs
import org.jetbrains.kotlin.kythe.signatures.KotlinSignaturesProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File

class KytheIndexerExtension(
    private val destination: String,
    private val corpus: String,
    private val root: String,
    private val target: String?,
    private val statisticsCollector: StatisticsCollector,
    private val fileVNames: FileVNames
) : AnalysisHandlerExtension {

    override fun analysisCompleted(
        project: Project,
        module: ModuleDescriptor,
        bindingTrace: BindingTrace,
        files: Collection<KtFile>
    ): AnalysisResult? {
        val compilationVName = getCompilationVName(files, target)
        val requiredInputs = getRequiredInputs(files, fileVNames)

        val psi2ir = Psi2IrTranslator(LanguageVersionSettingsImpl.DEFAULT, Psi2IrConfiguration(ignoreErrors = true))
        val generatorContext = psi2ir.createGeneratorContext(module, bindingTrace.bindingContext)
        val ir = psi2ir.generateModuleFragment(generatorContext, files)

        File(destination).outputStream().use { stream ->
            val factEmitter = StreamFactEmitter(stream)
            val indexer = IrBasedKytheIndexer(
                compilationVName,
                factEmitter,
                statisticsCollector,
                requiredInputs,
                PsiBasedSourcesManager(generatorContext.sourceManager, corpus, root),
                KotlinSignaturesProvider(generatorContext.symbolTable)
            )

            indexer.indexIrTree(ir)

            return AnalysisResult.success(bindingTrace.bindingContext, module, shouldGenerateCode = false)
        }
    }
}