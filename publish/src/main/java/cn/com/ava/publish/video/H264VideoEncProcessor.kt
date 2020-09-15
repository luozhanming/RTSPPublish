package cn.com.ava.publish.video

import android.media.MediaCodec
import android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME
import android.os.Build
import android.util.Log
import java.nio.ByteBuffer

class H264VideoEncProcessor(val mCodec: MediaCodec?) : Thread(), IVideoEncProcessor {

    companion object {
        const val TAG = "H264VideoEncProcessor"
    }

    private val mBufferInfo: MediaCodec.BufferInfo

    override var isStart: Boolean = false
        get() = field
        set(value) {
            field = value
        }
    override var isPausing: Boolean = false
        get() = field
        set(value) {
            field = value
        }

    override var mListener: IVEncListener? = null
        get() = field
        set(value) {
            field = value
        }

    override fun startProcess() {
        isStart = true
        start()
    }

    override fun stopProcess() {
        isStart = false
        join()
    }

    override fun pauseProcess() {
        isPausing = true
    }

    override fun resumeProcess() {
        isPausing = false
    }

    init {
        mBufferInfo = MediaCodec.BufferInfo()
    }


    override fun release() {
        stopProcess()
    }


    override fun run() {
        while (isStart) {
            while (isPausing) {
                sleep(100)
            }
            if(mCodec==null)return
            val outBufIndex = mCodec.dequeueOutputBuffer(mBufferInfo, 1000)
            if (outBufIndex >= 0) {
                var outputBuffer: ByteBuffer? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    outputBuffer = mCodec.getOutputBuffer(outBufIndex)
                } else {
                    val outputBuffers = mCodec.outputBuffers
                    outputBuffer = outputBuffers[outBufIndex]
                }
                val dataBytes = ByteArray(mBufferInfo.size)
                outputBuffer?.position(mBufferInfo.offset)
                outputBuffer?.limit(mBufferInfo.offset+mBufferInfo.size)
                outputBuffer?.get(dataBytes,mBufferInfo.offset,mBufferInfo.size)
                mListener?.onVData(dataBytes,mBufferInfo.flags==BUFFER_FLAG_KEY_FRAME,mBufferInfo.presentationTimeUs)
      //          file?.write(dataBytes)
                //处理数据
                mCodec.releaseOutputBuffer(outBufIndex, false)
            }else if(outBufIndex==MediaCodec.INFO_TRY_AGAIN_LATER){

            }else if(outBufIndex==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                Log.d(TAG, "Video encode :format output changed")
                val outputFormat = mCodec.getOutputFormat()
                val sps = outputFormat.getByteBuffer("csd-0").array()
                val pps = outputFormat.getByteBuffer("csd-1").array()
                mListener?.onSps(sps)
                mListener?.onPps(pps)
            }
        }
    }
}