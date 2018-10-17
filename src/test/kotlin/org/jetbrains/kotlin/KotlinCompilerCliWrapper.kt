package org.jetbrains.kotlin

import org.jetbrains.kotlin.KotlinCompiler.Companion.CLASSPATH_ARGUMENT
import org.jetbrains.kotlin.KotlinCompiler.Companion.DEFAULT_KYTHE_PLUGIN_PATH
import org.jetbrains.kotlin.KotlinCompiler.Companion.NO_STDLIB_ARGUMENT
import org.jetbrains.kotlin.KotlinCompiler.Companion.VERSION_ARGUMENT
import org.jetbrains.kotlin.KotlinCompiler.Companion.XPLUGIN
import org.jetbrains.kotlin.cli.common.CLITool
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.kythe.KytheIndexerCommandLineProcessor
import org.jetbrains.kotlin.kythe.KytheIndexerComponentRegistrar.Companion.KYTHE_CORPUS
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.lang.IllegalStateException
import java.net.URLClassLoader

interface KotlinCompiler {
    fun executeWithKythePlugin(inputFiles: List<File>, vararg kytheArguments: Pair<String, String>): ProcessOutput

    companion object {
        const val VERSION_ARGUMENT = "-version"
        const val XPLUGIN = "-Xplugin"
        const val NO_STDLIB_ARGUMENT = "-no-stdlib"
        const val CLASSPATH_ARGUMENT = "-classpath"


        const val DEFAULT_KOTLINC_PATH = "kotlinc-dist"
        val DEFAULT_KYTHE_PLUGIN_PATH = File("build/libs/kythe-indexer-1.0-SNAPSHOT-all.jar")
    }
}

class InProcessKotlinCompilerWrapper(private val kythePluginJar: File) : KotlinCompiler {
    override fun executeWithKythePlugin(inputFiles: List<File>, vararg kytheArguments: Pair<String, String>): ProcessOutput {
        val arguments = buildCompilerArguments(inputFiles, kytheArguments)
        return executeCompiler(K2JVMCompiler(), arguments)
    }

    // See org.jetbrains.kotlin.test.CompilerTestUtil.executeCompiler in the main Kotlin repo
    private fun executeCompiler(compiler: CLITool<*>, args: List<String>): ProcessOutput {
        val errBytes = ByteArrayOutputStream()
        val origErr = System.err

        val outBytes = ByteArrayOutputStream()
        val origOut = System.out
        try {
            System.setErr(PrintStream(errBytes))
            System.setOut(PrintStream(outBytes))
            val exitCode = CLITool.doMainNoExit(compiler, args.toTypedArray())
            return ProcessOutput(outBytes.toString(), errBytes.toString(), exitCode.code, args.joinToString(separator = " "))
        }
        finally {
            System.setErr(origErr)
            System.setOut(origOut)
        }
    }

    private fun buildCompilerArguments(inputFiles: List<File>, kytheArguments: Array<out Pair<String, String>>): List<String> {
        val files = inputFiles.map { it.absolutePath }

        // Load kythe plugin,
        val loadPlugin = listOf("$XPLUGIN=${kythePluginJar.absolutePath}")
        val kytheArgs = kytheArguments.flatMap { (argument, value) ->
            listOf("-P", constructPluginArgument(KytheIndexerCommandLineProcessor.PLUGIN_ID, argument, value))
        }

        // Here we're doing a bit shady things.
        //
        // We don't have a kotlin distribution, with proper directories layout ('lib', 'bin', etc), so we can't
        // just pass modified '-kotlin-home' to the compiler invocation.
        //
        // Instead, we have to:
        // a. Tell compiler to not complain about missing stdlib at '-kotlin-home' by passing '-no-stdlib'
        // b. Provide path to stdlib to the compiler (otherwise it won't be able to resolve declaration from stdlib
        //    in the analyzed code) via passing '-classpath <path-to-stdlib>'
        val noStdlib = listOf(NO_STDLIB_ARGUMENT)
        val addStdlibFromCurrentClasspath = listOf(CLASSPATH_ARGUMENT) + discoverPathToStdlibFromCurrentClasspath()
        return files + loadPlugin + kytheArgs + noStdlib + addStdlibFromCurrentClasspath
    }

    // This is a dirty hack, because actually stdlib against which user code should be analyzed can be different
    // from the stdlib against which kythe-plugin had been compiled.
    // Better solution would be to ask build system to provide path to stdlib explicitly
    private fun discoverPathToStdlibFromCurrentClasspath(): String {
        val currentClassloader = KotlinCompilerCliWrapper::class.java.classLoader as URLClassLoader
        return currentClassloader.urLs.filter { it.path.contains("kotlin-stdlib") }
                .joinToString(separator = ":") { it.path }
    }

    companion object {
        fun createWithDefaultPaths(): InProcessKotlinCompilerWrapper =
                InProcessKotlinCompilerWrapper(DEFAULT_KYTHE_PLUGIN_PATH)
    }
}

class KotlinCompilerCliWrapper(private val kotlincExecutable: String, private val kythePluginJar: File) : KotlinCompiler {
    init {
        val result = executeCommandAndGrabOutput("$kotlincExecutable $VERSION_ARGUMENT")
        require(result.exitValue == 0) {
            "Kotlinc invocation '${result.initialCommand}' returned value ${result.exitValue}"
        }

        require(kythePluginJar.exists()) {
            "Can't find Kythe compiler plugin jar at ${kythePluginJar.absolutePath}"
        }
    }

    override fun executeWithKythePlugin(inputFiles: List<File>, vararg kytheArguments: Pair<String, String>): ProcessOutput {
        val kythePluginArgumentsString = kytheArguments.joinToString(separator = " ") { (argument, value) ->
            "-P ${constructPluginArgument(KytheIndexerCommandLineProcessor.PLUGIN_ID, argument, value)}"
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

    companion object {
        fun createWithDefaultPaths(): KotlinCompilerCliWrapper =
                KotlinCompilerCliWrapper(KotlinCompiler.DEFAULT_KOTLINC_PATH, KotlinCompiler.DEFAULT_KYTHE_PLUGIN_PATH)
    }
}

private fun constructPluginArgument(pluginId: String, pluginArgumentName: String, pluginArgumentValue: String): String =
        "plugin:$pluginId:$pluginArgumentName=$pluginArgumentValue"