package test

//- @Foo defines/binding FooClass
//- FooClass.node/kind record
//- FooClass.subkind class
abstract class Foo {

    //- @bar defines/binding FooMethod
    //- FooMethod.node/kind function
    //- Anchor defines FooMethod
    //- Anchor.node/kind anchor
    //- Anchor.loc/start @^fun
    fun bar() {
        //- Anchor.loc/end @$"}"
    }

    //- FooAnchor.loc/end @$"}"
}