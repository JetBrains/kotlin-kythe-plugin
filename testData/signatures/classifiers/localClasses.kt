// !EXCLUDE: INSTANCE_RECEIVER, FAKE_OVERRIDE

class Outer {
    fun outer() {
        class LocalClass {
            fun foo() {}
        }
        LocalClass().foo()
    }
}