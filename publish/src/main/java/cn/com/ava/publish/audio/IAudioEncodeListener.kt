package cn.com.ava.publish.audio

/**
 * Listener for audio encoder.
 * */
interface IAudioEncodeListener {

    fun onAData(data:ByteArray, size:Int, offset:Int, timestamp:Long)

    fun onAudioError(err:Int)
    fun onAudioConfigure(configure: ByteArray)
}