package org.jetbrains.kotlin

import org.junit.Test

class SignaturesGeneratorTestGenerated : AbstractSignaturesGeneratorTest() {
    @Test
    fun testArrays() {
        doTest("testData/signatures/builtins/arrays.kt")
    }

    @Test
    fun testPrimitiveTypes() {
        doTest("testData/signatures/builtins/primitiveTypes.kt")
    }

    @Test
    fun testClasses() {
        doTest("testData/signatures/classifiers/classes.kt")
    }

    @Test
    fun testCompanionObject() {
        doTest("testData/signatures/classifiers/companionObject.kt")
    }

    @Test
    fun testEnum() {
        doTest("testData/signatures/classifiers/enum.kt")
    }

    @Test
    fun testEnumWithSecondaryCtor() {
        doTest("testData/signatures/classifiers/enumWithSecondaryCtor.kt")
    }

    @Test
    fun testInnerClass() {
        doTest("testData/signatures/classifiers/innerClass.kt")
    }

    @Test
    fun testLocalClasses() {
        doTest("testData/signatures/classifiers/localClasses.kt")
    }

    @Test
    fun testSealedClasses() {
        doTest("testData/signatures/classifiers/sealedClasses.kt")
    }

    @Test
    fun testSecondaryConstructorWithInitializersFromClassBody() {
        doTest("testData/signatures/classifiers/secondaryConstructorWithInitializersFromClassBody.kt")
    }

    @Test
    fun testSecondaryConstructors() {
        doTest("testData/signatures/classifiers/secondaryConstructors.kt")
    }

    @Test
    fun testLocalFun() {
        doTest("testData/signatures/methods/localFun.kt")
    }

    @Test
    fun testFromConstructor() {
        doTest("testData/signatures/properties/fromConstructor.kt")
    }

    @Test
    fun testSimpleVal() {
        doTest("testData/signatures/properties/simpleVal.kt")
    }

    @Test
    fun testSimpleVar() {
        doTest("testData/signatures/properties/simpleVar.kt")
    }

    @Test
    fun testCompanion() {
        doTest("testData/signatures/singletons/companion.kt")
    }

    @Test
    fun testEnumEntry() {
        doTest("testData/signatures/singletons/enumEntry.kt")
    }

    @Test
    fun testObject() {
        doTest("testData/signatures/singletons/object.kt")
    }

    @Test
    fun testClass() {
        doTest("testData/signatures/typeParameters/class.kt")
    }

    @Test
    fun testConstructor() {
        doTest("testData/signatures/typeParameters/constructor.kt")
    }

    @Test
    fun testGenericInnerClass() {
        doTest("testData/signatures/typeParameters/genericInnerClass.kt")
    }

    @Test
    fun testMethodWithTypeParameter() {
        doTest("testData/signatures/typeParameters/methodWithTypeParameter.kt")
    }

    @Test
    fun testShadowing() {
        doTest("testData/signatures/typeParameters/shadowing.kt")
    }

    @Test
    fun testTypeParameterBoundedBySubclass() {
        doTest("testData/signatures/typeParameters/typeParameterBoundedBySubclass.kt")
    }
}
