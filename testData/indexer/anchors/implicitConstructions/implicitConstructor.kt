package Test

//- @Foo defines/binding FooClass
//- FooClass.node/kind record
//- FooClass.subkind class
//- @Foo defines/binding FooCtor
//- FooCtor.node/kind function
//- FooAnchor defines FooCtor
//- FooAnchor.node/kind anchor
//- FooAnchor.loc/start @^class
//- FooAnchor.loc/end @$Foo
class Foo