package cn.com.ava.publish.audio


/**
 * Listener for audio Record by AudioRecorder.
 * */
interface IAudioRecListener {

    fun onData(data:ByteArray,size:Int)

    fun onError(err:Int)
}