package cn.com.ava.publish.audio

import android.media.MediaCodec
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import cn.com.ava.publish.configuration.AudioConfiguration
import cn.com.ava.publish.util.Utils
import java.nio.ByteBuffer
import kotlin.system.measureTimeMillis

class AudioEncoder(val mAudioConfiguration: AudioConfiguration) {

    companion object {
        const val TAG = "AudioEncoder"
    }

    private var mCodec: MediaCodec

    private lateinit var mEncodeHandler: Handler

    private var mEncodeThread: HandlerThread

    private var startTimeStamp: Long = 0

    private var mAudioEncodeListener: IAudioEncodeListener? = null

    private var isStart:Boolean = false

    private var mDrainProcessor:DrainEncodeDataProcessor? = null

    private var mBufInfo:MediaCodec.BufferInfo


    init {
        mCodec =Utils.getAudioMediaCodec(mAudioConfiguration)
        mBufInfo = MediaCodec.BufferInfo()
        mEncodeThread = HandlerThread("audio_encode")
        mDrainProcessor = DrainEncodeDataProcessor()
    }


    fun prepareEncode() {
        mEncodeThread.start()
        mEncodeHandler = Handler(mEncodeThread.looper)
        mCodec.start()
        startTimeStamp = System.currentTimeMillis()
        isStart = true
        mDrainProcessor?.start()
    }

    fun stop() {
        mEncodeThread.quitSafely()
        mEncodeHandler.removeCallbacksAndMessages(null)
        isStart = false
        mDrainProcessor?.join()
        mCodec.stop()
    }


    fun offerEncode(data: ByteArray, size: Int) {
        mEncodeHandler.post {
            val measureTimeMillis = measureTimeMillis {
                val inBufIndex = mCodec.dequeueInputBuffer(1000)
                if (inBufIndex != -1) {
                    val inBuf: ByteBuffer?
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        inBuf = mCodec.getInputBuffer(inBufIndex)
                    } else {
                        val inputBuffers = mCodec.inputBuffers
                        inBuf = inputBuffers[inBufIndex]
                    }
                    if (inBuf != null) {
                        //填充数据
                        inBuf.clear()
                        inBuf.put(data)
                        mCodec.queueInputBuffer(
                            inBufIndex,
                            0,
                            size,
                            System.nanoTime()/1000L,
                            0
                        )
                    }
                }

            }
            Log.d("messureTimeMillis", "$measureTimeMillis")
        }

    }

    fun release() {
        stop()
        mCodec.release()
        if(mDrainProcessor?.isAlive == true){
            mDrainProcessor?.join()
        }
        mDrainProcessor = null
    }


    fun setListener(listener: IAudioEncodeListener) {
        this.mAudioEncodeListener = listener
    }



    inner class DrainEncodeDataProcessor :Thread(){

        override fun run() {
            while (isStart){
                var outBufIndex = mCodec.dequeueOutputBuffer(mBufInfo, 1000)
                if (outBufIndex >= 0) {
                    val outBuf: ByteBuffer?
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        outBuf = mCodec.getOutputBuffer(outBufIndex)
                    } else {
                        val outputBuffers = mCodec.outputBuffers
                        outBuf = outputBuffers[outBufIndex]
                    }
                    val outSize = mBufInfo.size
                    val offset = mBufInfo.offset
                    val presentationTimeUs = mBufInfo.presentationTimeUs
                    //处理编码后的输出
                    outBuf?.position(offset)
                    outBuf?.limit(offset+mBufInfo.size)
                    val ret = ByteArray(outSize)
                    outBuf?.get(ret,offset,mBufInfo.size)
                    mAudioEncodeListener?.onAData(ret, outSize, offset, presentationTimeUs)
                    mCodec.releaseOutputBuffer(outBufIndex, false)
                }else if (outBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {   //输出改变
                    val outputFormat = mCodec.getOutputFormat()
                    val configure = outputFormat.getByteBuffer("csd-0").array()
                    mAudioEncodeListener?.onAudioConfigure(configure)
                } else if (outBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {

                }
                sleep(2)
            }
        }
    }
}