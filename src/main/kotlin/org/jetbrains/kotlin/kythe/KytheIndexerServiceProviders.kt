/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kythe

import com.google.devtools.kythe.extractors.shared.FileVNames
import com.google.devtools.kythe.platform.shared.NullStatisticsCollector
import com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.kythe.indexer.getEnvironment
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension

object KytheIndexerConfigurationKeys {
    val OUTPUT: CompilerConfigurationKey<String> = CompilerConfigurationKey.create("Destination of compilation index")
    val ROOT: CompilerConfigurationKey<String> = CompilerConfigurationKey.create("Corpus root")
    val TEST_SIGNATURES: CompilerConfigurationKey<Boolean> = CompilerConfigurationKey.create("Run signatures generator test")
}

object KytheIndexerCliOptions {
    val OUTPUT = CliOption(
            name = "output",
            valueDescription = "<path>",
            description = "Path to save emitted entries",
            required = false
    )

    val ROOT = CliOption(
            name = "root",
            valueDescription = "<path>",
            description = "Corpus root",
            required = false
    )

    val TEST_SIGNATURES = CliOption(
            name = "test-signatures",
            valueDescription = "<path>",
            description = "Path to file with test-data on signatures",
            required = false
    )
}

class KytheIndexerComponentRegistrar : ComponentRegistrar {

    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val corpus = getEnvironment(KYTHE_CORPUS) ?: DEFAULT_CORPUS
        val fileVNames = getEnvironment(KYTHE_VNAMES)?.let { FileVNames.fromFile(it) }
                ?: FileVNames.staticCorpus(corpus)
        val definedTarget = getEnvironment(KYTHE_ANALYSIS_TARGET)

        val extension = if (configuration.get(KytheIndexerConfigurationKeys.TEST_SIGNATURES) == true) {
            SignatureTestExtension()
        } else {
            KytheIndexerExtension(
                    configuration.getNotNull(KytheIndexerConfigurationKeys.OUTPUT),
                    corpus,
                    configuration.getNotNull(KytheIndexerConfigurationKeys.ROOT),
                    definedTarget,
                    NullStatisticsCollector.getInstance(),
                    fileVNames
            )
        }

        AnalysisHandlerExtension.registerExtension(project, extension)
    }

    companion object {
        const val KYTHE_ANALYSIS_TARGET = "KYTHE_ANALYSIS_TARGET"
        const val KYTHE_VNAMES = "KYTHE_VNAMES"
        const val KYTHE_CORPUS = "KYTHE_CORPUS"
        const val DEFAULT_CORPUS = "kythe"
    }
}

class KytheIndexerCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = PLUGIN_ID
    override val pluginOptions: Collection<CliOption> = listOf(
            KytheIndexerCliOptions.OUTPUT,
            KytheIndexerCliOptions.ROOT,
            KytheIndexerCliOptions.TEST_SIGNATURES
    )

    override fun processOption(option: CliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            KytheIndexerCliOptions.OUTPUT ->
                configuration.put(KytheIndexerConfigurationKeys.OUTPUT, value)

            KytheIndexerCliOptions.ROOT ->
                configuration.put(KytheIndexerConfigurationKeys.ROOT, value)

            KytheIndexerCliOptions.TEST_SIGNATURES ->
                configuration.put(KytheIndexerConfigurationKeys.TEST_SIGNATURES, value.toBoolean())

            else -> throw CliOptionProcessingException("Unknown option: ${option.name}")
        }
    }

    companion object {
        const val PLUGIN_ID = "kythe-indexer"
    }
}