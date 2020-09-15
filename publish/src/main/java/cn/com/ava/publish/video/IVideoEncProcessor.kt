package cn.com.ava.publish.video

interface IVideoEncProcessor {

    var isStart:Boolean

    var isPausing:Boolean

    var mListener: IVEncListener?

    fun startProcess()

    fun stopProcess()

    fun pauseProcess()

    fun resumeProcess()

    fun release()
}