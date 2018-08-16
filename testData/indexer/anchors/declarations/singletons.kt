package test

//- @Obj defines/binding Object
//- Object.node/kind constant
//- Object.subkind object
//- ObjAnchor defines Object
//- ObjAnchor.node/kind anchor
//- ObjAnchor.loc/start @^object
object Obj {
    //- ObjAnchor.loc/end @$"}"
}

class Foo {
    //- @object defines/binding Companion
    //- Companion.node/kind constant
    //- Companion.subkind companion
    //- CompAnchor defines Companion
    //- CompAnchor.node/kind anchor
    //- CompAnchor.loc/start @^companion
    companion object {
        //- CompAnchor.loc/end @$"}"
    }
}

class Bar {
    //- @NamedCompanion defines/binding Named
    //- Named.node/kind constant
    //- Named.subkind companion
    //- NamedAnchor defines Named
    //- NamedAnchor.node/kind anchor
    //- NamedAnchor.loc/start @^companion
    companion object NamedCompanion {
        //- NamedAnchor.loc/end @$"}"
    }
}