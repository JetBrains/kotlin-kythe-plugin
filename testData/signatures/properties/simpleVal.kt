package foo.bar.baz

class MyInt

class Bar {
    private val justVal: MyInt? = null

    private val valWithGetter: MyInt?
        get() = null

    fun test() {
        val local: MyInt? = null
    }
}