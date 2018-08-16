package test

//- @Foo defines/binding FooClass
//- FooClass.node/kind record
//- FooClass.subkind class
//-
//- FooAnchor defines FooClass
//- FooAnchor.node/kind anchor
//- FooAnchor.loc/start @^abstract
abstract class Foo {
    //- FooAnchor.loc/end @$"}"
}