package test

//- @x defines/binding X
//- X.node/kind variable
//- X.subkind local/parameter
//- Anchor defines X
//- Anchor.node/kind anchor
//- Anchor.loc/start @^x
//- Anchor.loc/end @$Int
fun justFun(x: Int) {

}

//- @y defines/binding Y
//- Y.node/kind variable
//- Y.subkind local/parameter
//- AnchorY defines Y
//- AnchorY.node/kind anchor
//- AnchorY.loc/start @^y
//- AnchorY.loc/end @$Int
//-
//- @Double defines/binding Receiver
//- Receiver.node/kind variable
//- Receiver.subkind local/parameter
fun Double.extensionFun(y: Int) {

}