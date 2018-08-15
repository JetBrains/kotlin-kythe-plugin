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
}

object KytheIndexerCliOptions {
    val OUTPUT = CliOption(
        name = "output",
        valueDescription = "<path>",
        description = "Path to save emitted entries"
    )
}

class KytheIndexerComponentRegistrar : ComponentRegistrar {

    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val corpus = getEnvironment(KYTHE_CORPUS) ?: DEFAULT_CORPUS
        val fileVNames = getEnvironment(KYTHE_VNAMES)?.let { FileVNames.fromFile(it) }
                ?: FileVNames.staticCorpus(corpus)

        val definedTarget = getEnvironment(KYTHE_ANALYSIS_TARGET)

        AnalysisHandlerExtension.registerExtension(
            project,
            KytheIndexerExtension(
                configuration.getNotNull(KytheIndexerConfigurationKeys.OUTPUT),
                corpus,
                "",
                definedTarget,
                NullStatisticsCollector.getInstance(),
                fileVNames
            )
        )
    }

    companion object {
        const val KYTHE_ANALYSIS_TARGET = "KYTHE_ANALYSIS_TARGET"
        const val KYTHE_VNAMES = "KYTHE_VNAMES"
        const val KYTHE_CORPUS = "KYTHE_CORPUS"
        const val DEFAULT_CORPUS = "kythe"
    }
}

class KytheIndexerCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = "kythe-indexer"
    override val pluginOptions: Collection<CliOption> = listOf(KytheIndexerCliOptions.OUTPUT)

    override fun processOption(option: CliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            KytheIndexerCliOptions.OUTPUT -> configuration.put(KytheIndexerConfigurationKeys.OUTPUT, value)
            else -> throw CliOptionProcessingException("Unknown option: ${option.name}")
        }
    }
}