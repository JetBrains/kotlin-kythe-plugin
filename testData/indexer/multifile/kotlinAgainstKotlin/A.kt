package a

//- FileA=vname("","test","testData/indexer/multifile/kotlinAgainstKotlin","A.kt","")
//-  .node/kind file
//- FileB=vname("","test","testData/indexer/multifile/kotlinAgainstKotlin","B.kt","")
//-  .node/kind file

//- @A defines/binding ClassA
//- ClassA.node/kind record
//- ClassA.subkind class
//- ClassA childof FileA
abstract class A {
    //- @"B" ref ClassB
    //- ClassB.node/kind record
    //- ClassB childof FileB
    abstract val b: B
}