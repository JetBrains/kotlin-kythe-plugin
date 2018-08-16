/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import java.io.File

abstract class AbstractKytheIndexTest {
    protected fun doTest(filePath: String) {
        val testDataFile = File(filePath)
        val indexFile = File.createTempFile("kotlin-kythe", ".index")

        if (testDataFile.isDirectory) {
            doTest(indexFile, testDataFile.listFiles().asList())
        } else {
            doTest(indexFile, listOf(testDataFile))
        }
    }

    private fun doTest(indexFile: File, inputFiles: List<File>) {
        runKotlinCompilerWithKythePlugin(inputFiles, indexFile)
        for (file in inputFiles) {
            feedFileIntoKytheVerifier(file, indexFile)
        }
    }

    private fun runKotlinCompilerWithKythePlugin(inputFiles: List<File>, indexFile: File): String {
        val compilerWrapper = KotlinCompilerCliWrapper.createWithDefaultPaths()
        return compilerWrapper.executeWithKythePlugin(
                inputFiles,
                KythePluginArguments.OUTPUT to indexFile.absolutePath,
                KythePluginArguments.ROOT to inputFiles.first().parentFile.path
        ).assertZeroExitCode() // Do not assert stderr, as warnings are written here
    }

    private fun feedFileIntoKytheVerifier(inputFile: File, indexFile: File) {
        val processBuilder = ProcessBuilder(KYTHE_VERIFIER, inputFile.absolutePath, KytheVerifierFlags.IGNORE_DUPS)
                .redirectInput(indexFile)
        val command = processBuilder.command().joinToString(" ")
        val process = processBuilder.start()

        process.waitFor()

        val processOutput = ProcessOutput(process, command)

        val graphDumpFile = File(indexFile.path.replace(".index", ".graph"))
        require(graphDumpFile != indexFile)
        graphDumpFile.writeText(processOutput.output, Charsets.UTF_8)

        processOutput.assertZeroExitCode()
    }

    companion object {
        const val KYTHE_VERIFIER = "/opt/kythe/tools/verifier"
    }
}