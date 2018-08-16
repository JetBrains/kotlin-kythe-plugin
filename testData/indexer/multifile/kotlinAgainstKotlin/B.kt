package a

//- FileA=vname("","test","testData/indexer/multifile/kotlinAgainstKotlin","A.kt","")
//-  .node/kind file
//- FileB=vname("","test","testData/indexer/multifile/kotlinAgainstKotlin","B.kt","")
//-  .node/kind file

//- @B defines/binding ClassB
//- ClassB.node/kind record
//- ClassB.subkind class
//- ClassB childof FileB
abstract class B {
    //- @"A" ref ClassA
    //- ClassA.node/kind record
    //- ClassA childof FileA
    abstract val a: A
}