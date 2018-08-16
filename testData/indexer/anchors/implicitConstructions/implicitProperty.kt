package test

//- @x defines/binding XParam
//- XParam.node/kind variable
//- XParam.subkind local/parameter
//-
//- @x defines/binding XProperty
//- @"var x: Int" defines XProperty
//- XProperty.node/kind property
//-
//- @x defines/binding XGetter
//- @"var x: Int" defines XGetter
//- XGetter.node/kind function
//-
//- @x defines/binding XSetter
//- @"var x: Int" defines XSetter
//- XSetter.node/kind function
class Foo(var x: Int) {

}