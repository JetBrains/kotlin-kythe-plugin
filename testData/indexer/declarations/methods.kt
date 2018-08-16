package test

//- @Foo defines/binding FooClass
abstract class Foo {
    //- EmptyToUnit.node/kind tapp
    //- EmptyToUnit param.0 FnBuiltin=vname("fn#builtin", _, _, _, _)
    //- EmptyToUnit param.1 KtUnit=vname("OBJ:kotlin.Unit", _, _, _, _)

    //- @foo defines/binding FooMethod
    //- FooMethod.node/kind function
    //- FooMethod childof FooClass
    //- FooMethod typed EmptyToUnit
    fun foo() {

    }

    //- FooAnchor.loc/end @$"}"
}