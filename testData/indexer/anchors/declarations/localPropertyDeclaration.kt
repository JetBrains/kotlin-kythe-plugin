package test

fun foo() {
    //- @x defines/binding X
    //- X.node/kind variable
    //- X.subkind local
    //- Anchor defines X
    //- Anchor.node/kind anchor
    //- Anchor.loc/start @^val
    //- Anchor.loc/end @$"42"
    val x: Int = 42
}