package org.jetbrains.kotlin

import org.jetbrains.kotlin.kythe.KytheIndexerCommandLineProcessor
import org.jetbrains.kotlin.kythe.KytheIndexerComponentRegistrar.Companion.KYTHE_CORPUS
import java.io.File

class KotlinCompilerCliWrapper(private val kotlincExecutable: String, private val kythePluginJar: File) {
    init {
        val result = executeCommandAndGrabOutput("$kotlincExecutable $VERSION_ARGUMENT")
        require(result.exitValue == 0) {
            "Kotlinc invocation '${result.initialCommand}' returned value ${result.exitValue}"
        }

        require(kythePluginJar.exists()) {
            "Can't find Kythe compiler plugin jar at ${kythePluginJar.absolutePath}"
        }
    }

    fun executeWithKythePlugin(inputFiles: List<File>, vararg kytheArguments: Pair<String, String>): ProcessOutput {
        val kythePluginArgumentsString = kytheArguments.joinToString(separator = " ") { (argument, value) ->
            constructPluginArgument(KytheIndexerCommandLineProcessor.PLUGIN_ID, argument, value)
        }

        val inputFilesString = inputFiles.joinToString(separator = " ")

        // Something like:
        // > kotlinc Foo.kt -Xplugin=build/libs/kythe-indexer-1.0-SNAPSHOT-all.jar -P plugin:kythe-indexer:output=out.index
        val cliInvocation = "$kotlincExecutable " +
                "$inputFilesString " +
                "$XPLUGIN=${kythePluginJar.absolutePath} " +
                kythePluginArgumentsString

        return executeCommandAndGrabOutput(cliInvocation) {
            environment()[KYTHE_CORPUS] = "test"
        }
    }

    private fun constructPluginArgument(pluginId: String, pluginArgumentName: String, pluginArgumentValue: String): String =
            "-P plugin:$pluginId:$pluginArgumentName=$pluginArgumentValue"

    companion object {
        const val VERSION_ARGUMENT = "-version"
        const val KYTHE_PLUGIN_ID = "kythe-indexer"
        const val XPLUGIN = "-Xplugin"

        const val DEFAULT_KOTLINC_PATH = "kotlinc-dist"
        val DEFAULT_KYTHE_PLUGIN_PATH = File("build/libs/kythe-indexer-1.0-SNAPSHOT-all.jar")

        fun createWithDefaultPaths(): KotlinCompilerCliWrapper =
                KotlinCompilerCliWrapper(DEFAULT_KOTLINC_PATH, DEFAULT_KYTHE_PLUGIN_PATH)
    }
}