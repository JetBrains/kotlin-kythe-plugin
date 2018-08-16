// !EXCLUDE: INSTANCE_RECEIVER, FAKE_OVERRIDE

class Outer {
    open inner class TestInnerClass

    inner class DerivedInnerClass : TestInnerClass()
}
