// !EXCLUDE: INSTANCE_RECEIVER, FAKE_OVERRIDE

interface TestInterface<T> {
    interface TestNestedInterface<TT>
}

class Test<T0> {
    class TestNested<T1>
    inner class TestInner<T2>
}