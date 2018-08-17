# kotlin-kythe

Strawman prototype of Kythe indexer for Kotlin indexer

## Current state

Current work is a small prototype, main purpose of which is to kickstart work
on supporting Kotlin in Kythe by showing how to 
deal with Kotlin compiler internal structures, extract needed information, etc.

Support of Kotlin language isn't complete in this prototype. However, we expect
that unsupported constructions are either very easy to support (essentially by adding
overload in visitor and re-using existing method) or are advanced features (typealiases, 
reified generics, etc.). It means that we hope that this prototype is relatively close
to support core Kotlin language.

Support of Kythe semantics is a bit more lacking. Prototype emits nodes and corresponding
edges for declarations, anchors, abstract nodes, type references, class hierarchies. 

Notably, sources and diagnostics are currently missing. Also, current implementation doesn't 
emit corresponding JVM nodes for Kotlin declarations.

Finally, we feel that there are a hefty amount of design work regarding how Kotlin language
semantics should be embedded in Kythe model, like: should some kinds of declarations which unique to 
Kotlin (objects, companion objects, properties) have separate node kind/subkind?

## Implementation details

Here we briefly describe architecture of Kotlin-kythe and related things about Kotlin compiler

### Compiler Plugins

Kythe support is built as a compiler plugin, meaning that current implementation is assembled to `.jar`
which then should be passed to `kotlinc`. Plugin will register some extension point, in which compiler
will call back, allowing to analyze user code.

See [documentation](https://kotlinlang.org/docs/reference/compiler-plugins.html) on some basic information
how to launch `kotlinc` with plugin. 

### Compiler infrastructure

Compilation process:
 * First lexing and parsing performed, resultng in 
structure, known as PSI-tree. This is a purely syntactical tree, i.e. there's no type information in it
yet
 * Then, compiler builds **descriptors** -- essentially, compiler symbols
 * Then, name resolution and inference is launched to map syntactic nodes of PSI tree to corresponding
 descriptors. This mapping (and all other type information) is stored in a structure called `BindingContext`:
 essentially, this is a map from PSI-nodes to various frontend information
 * Then, backend consumes pair (PSI-tree, `BindingContext`) and generates bytecode. Currently, there are two 
 options:
    * Use just PSI-tree and `BindingContext`. This has been proved to be not very convenient, because information
    in PSI is purely syntactical which leads to a lot of code duplication (e.g. for desugaring some complex Kotlin
    constructions). 
    * Build intermediate representation (IR) in a tree-ish form with type information already present in nodes. 
    Advantage of this approach is that some desugraings are already applied, exposing code semantics in a more
    accessible way.
    
Currently, Kotlin compiler uses "PSI + `BindingContext`" approach in production, and IR is under development. 
Still, for Kythe-plugin we use IR-approach, because it's much more convenient to work with it than with PSI.

However, IR is built for the compiler backend, so it may lack some information (notably, support for sources and 
diagnostics is minimal). Therefore, Kythe-plugin implementation sometimes returns back to PSI to
pull some additional information.

### Kythe-plugin infrastructure

Generally, it consists of two main classes: `SignaturesProvider`, which generates stable signatures for Kotlin 
symbols (which then can be used for VNames) and `IrBasedKytheIndexer`, which visits given IR-tree and emits
stream of Kythe nodes

Current implementation depends on some of the common Kythe functionality, like entities emitters, node kinds, etc. 
Those sources are just copy-pasted but are kept in a separate module, to make porting Kotlin-Kythe easier. 
Some changes were applied to this code -- they are quite minimal, and marked as `[Kythe-common patch]` in git history. 

To make work with prototype easier, we build Kythe-plugin as fat-jar, though that's not necessary of course.

Also, current test infrastructure forks process and launches Kotlin-compiler from CLI -- it was the fastest
and easiest solution we've come out, because setting up compiler environment is a non-trivial task, and we were
not sure that it is needed at all.
