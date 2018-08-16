package test

class Foo {

    //- @x defines/binding X
    //- X.node/kind property
    //- @x defines/binding GetX
    //- GetX.node/kind function
    //- AnchorX defines GetX
    //- AnchorX.node/kind anchor
    //- AnchorX.loc/start @^val
    //- AnchorX.loc/end @$"42"
    val x: Int = 42

    //- @y defines/binding Y
    //- Y.node/kind property
    //-
    //- @y defines/binding GetY
    //- GetY.node/kind function
    //- AnchorY defines GetY
    //- AnchorY.node/kind anchor
    //- AnchorY.loc/start @^var
    //- AnchorY.loc/end @$"43"
    //-
    //- @y defines/binding SetY
    //- SetY.node/kind function
    //- AnchorY defines SetY
    var y: Int = 43
}