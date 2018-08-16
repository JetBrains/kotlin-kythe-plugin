//- @GenericClass defines/binding GAbs
//- @T defines/binding TVar
//- GAbs.node/kind abs
//- GAbs param.0 TVar
//- TVar.node/kind absvar
//- Class childof GAbs
class GenericClass<T> {

    fun foo() {
        //- @GenericClass ref GAbs
        //- @x defines/binding Var
        //- Var.node/kind variable
        //- Var.subkind local
        //-
        //- Var typed OType
        //- OType.node/kind tapp
        //- OType param.0 GAbs
        //- OType param.1 StringType=vname("CLASS:kotlin.String", _, _, _, _)
        val x: GenericClass<String>
    }
}
