// !EXCLUDE: INSTANCE_RECEIVER, FAKE_OVERRIDE

class C {
    constructor() : this(0) {}
    constructor(x: Int) {}
}