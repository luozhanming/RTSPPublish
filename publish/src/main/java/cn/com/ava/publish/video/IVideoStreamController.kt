package cn.com.ava.publish.video

import android.view.Surface

interface IVideoStreamController {

    var mListener:IVEncListener?




    var mVideoEncProcessor:IVideoEncProcessor?

    fun startEncoding()

    fun stopEncoding()

    fun resumeEncoding()

    fun pauseEncoding()

    fun offerEncode(data: ByteArray, timestamp: Long)

    fun release()

    fun setInputSurface(surface: Surface)

    fun getInputSurface():Surface?

}