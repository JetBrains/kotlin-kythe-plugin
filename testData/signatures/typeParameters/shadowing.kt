// !EXCLUDE: INSTANCE_RECEIVER, FAKE_OVERRIDE

class Host<T> {

    fun capturedFromHost(x: T) {}

    fun <T> shadowsHostParam(j: T) {}
}