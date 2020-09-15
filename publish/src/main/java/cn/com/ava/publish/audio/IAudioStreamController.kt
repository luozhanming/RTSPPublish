package cn.com.ava.publish.audio

interface IAudioStreamController {

    var isMute:Boolean

    fun startRecording()

    fun stopRecording()

    fun resume()

    fun pause()

    fun release()

    fun setListener(listener: IAudioEncodeListener?)


}