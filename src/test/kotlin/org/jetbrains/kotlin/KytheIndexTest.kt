package org.jetbrains.kotlin

import org.junit.Test

class KytheIndexTestGenerated : AbstractKytheIndexTest() {
    @Test
    @Throws(Exception::class)
    fun testAbstractClass() {
        doTest("testData/indexer/anchors/declarations/abstractClass.kt")
    }

    @Test
    @Throws(Exception::class)
    fun testClassDeclaration() {
        doTest("testData/indexer/anchors/declarations/classDeclaration.kt")
    }

    @Test
    @Throws(Exception::class)
    fun testLocalPropertyDeclaration() {
        doTest("testData/indexer/anchors/declarations/localPropertyDeclaration.kt")
    }

    @Test
    @Throws(Exception::class)
    fun testMethodDeclaration() {
        doTest("testData/indexer/anchors/declarations/methodDeclaration.kt")
    }

    @Test
    @Throws(Exception::class)
    fun testParameters() {
        doTest("testData/indexer/anchors/declarations/parameters.kt")
    }

    @Test
    @Throws(Exception::class)
    fun testPropertyDeclaration() {
        doTest("testData/indexer/anchors/declarations/propertyDeclaration.kt")
    }

    @Test
    @Throws(Exception::class)
    fun testSingletons() {
        doTest("testData/indexer/anchors/declarations/singletons.kt")
    }

    @Test
    @Throws(Exception::class)
    fun testImplicitAccessors() {
        doTest("testData/indexer/anchors/implicitConstructions/implicitAccessors.kt")
    }

    @Test
    @Throws(Exception::class)
    fun testImplicitConstructor() {
        doTest("testData/indexer/anchors/implicitConstructions/implicitConstructor.kt")
    }

    @Test
    @Throws(Exception::class)
    fun testImplicitProperty() {
        doTest("testData/indexer/anchors/implicitConstructions/implicitProperty.kt")
    }

    @Test
    @Throws(Exception::class)
    fun testGenericClass() {
        doTest("testData/indexer/declarations/genericClass.kt")
    }

    @Test
    @Throws(Exception::class)
    fun testMethods() {
        doTest("testData/indexer/declarations/methods.kt")
    }

    @Test
    @Throws(Exception::class)
    fun testKotlinAgainstKotlin() {
        doTest("testData/indexer/multifile/kotlinAgainstKotlin")
    }
}
