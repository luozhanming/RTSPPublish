package cn.com.ava.publish.video

interface IVEncListener {
    fun onVData(
        data: ByteArray,
        isKeyFrame: Boolean,
        presentationTime: Long
    )
    fun onVideoError()
    fun onSps(sps: ByteArray)
    fun onPps(pps: ByteArray)
}