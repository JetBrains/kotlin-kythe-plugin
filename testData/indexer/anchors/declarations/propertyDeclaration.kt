package test

//- @Foo defines/binding FooClass
abstract class Foo {

    //- @bar defines/binding BarProp
    //- BarProp.node/kind property
    //- BarAnchor defines BarProp
    //- BarAnchor.node/kind anchor
    //- BarAnchor.loc/start @^val
    val bar: Int = 42
        //- @get defines/binding BarGet
        //- BarGet.node/kind function
        //- GetterAnchor defines BarGet
        //- GetterAnchor.node/kind anchor
        //- GetterAnchor.loc/start @^get
        get() {
            return field + 1
            //- GetterAnchor.loc/end @$"}"
            //- BarAnchor.loc/end @$"}"
        }
}