package org.jetbrains.kotlin

import java.io.InputStream

data class ProcessOutput(val output: String, val errors: String, val exitValue: Int, val initialCommand: String) {
    constructor(process: Process, initialCommand: String) :
            this(
                    process.inputStream.readToStringCompletely(),
                    process.errorStream.readToStringCompletely(),
                    process.exitValue(),
                    initialCommand
            )
}

fun executeCommandAndGrabOutput(command: String, builder: ProcessBuilder.() -> Unit = {}): ProcessOutput {
    val process = ProcessBuilder(command.split(" ")).apply(builder).start()
    process.waitFor()
    return ProcessOutput(process, command)
}

fun ProcessOutput.assertSuccessful(): String {
    if (errors.isNotEmpty() || exitValue != 0) {
        throw AssertionError("Command $initialCommand finished ungracefully.\n" +
                "Output: $output" +
                "Stderr: $errors\n" +
                "Exit code: $exitValue\n"
        )
    }

    return output
}

fun ProcessOutput.assertZeroExitCode(): String {
    if (exitValue != 0) {
        throw AssertionError("Command $initialCommand finished ungracefully.\n" +
                "Output: $output" +
                "Stderr: $errors\n" +
                "Exit code: $exitValue\n"
        )
    }

    return output
}

private fun InputStream.readToStringCompletely(): String = bufferedReader(Charsets.UTF_8).use { it.readText() }

object KytheVerifierFlags {
    const val GRAPHVIZ: String = "-graphviz"
    const val ANNOTATED_GRAPHVIZ: String = "-annotated_graphviz"
    const val IGNORE_DUPS: String = "-ignore_dups"
    const val SHOW_GOALS: String = "-show_goals"
    const val SHOW_PROTOS: String = "-show_protos"
}

object KythePluginArguments {
    const val OUTPUT = "output"
    const val ROOT = "root"
    const val TEST_SIGNATURES_ARGUMENT = "test-signatures"
}