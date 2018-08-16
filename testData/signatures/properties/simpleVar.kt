package foo.bar.baz

class MyInt

class Bar {
    private var justvar: MyInt? = null

    private var varWithGetter: MyInt? = null
        get() = null

    private var varWithSetter: MyInt? = null
        set(x) { }

    private var varWithSetterAndGetter: MyInt?
        get() = null
        set(x) { }

    fun test() {
        var local: MyInt? = null
    }
}