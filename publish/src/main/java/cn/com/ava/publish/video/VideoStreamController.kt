package cn.com.ava.publish.video

import android.media.MediaCodec
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import androidx.annotation.RequiresApi
import cn.com.ava.publish.configuration.VideoConfiguration
import cn.com.ava.publish.util.Utils
import java.nio.ByteBuffer

class VideoStreamController(mVideoConfiguration: VideoConfiguration) : IVideoStreamController {


    private var mPPS: ByteArray? = null

    private var mSPS: ByteArray? = null

    private val mEncodeThread: HandlerThread by lazy {
        val thread = HandlerThread("EncodeVideo")
        thread.start()
        thread
    }

    private val mEncodeHandler: Handler by lazy {
        Handler(mEncodeThread.looper)
    }

    private var mCodec: MediaCodec? = null
        get() = field

    override var mListener: IVEncListener? = null
        get() = field
        set(value) {
            field = value
        }

    override var mVideoEncProcessor: IVideoEncProcessor? = null
        get() = field
        set(value) {
            field = value
        }


    init {
        mCodec = Utils.getVideoMediaCodec(mVideoConfiguration)
        mVideoEncProcessor = H264VideoEncProcessor(mCodec)
        mVideoEncProcessor?.mListener =
            object : IVEncListener {
                override fun onVData(
                    data: ByteArray,
                    isKeyFrame: Boolean,
                    presentationTime: Long
                ) {
                    mListener?.onVData(data, isKeyFrame, presentationTime)
                }

                override fun onVideoError() {
                    mListener?.onVideoError()
                }

                override fun onSps(sps: ByteArray) {
                    mSPS = sps
                    mListener?.onSps(sps)
                }

                override fun onPps(pps: ByteArray) {
                    mPPS = pps
                    mListener?.onPps(pps)
                }
            }

    }


    override fun startEncoding() {
        mCodec?.start()
        mVideoEncProcessor?.startProcess()
    }

    override fun stopEncoding() {
        mVideoEncProcessor?.stopProcess()
        mCodec?.stop()
    }

    override fun resumeEncoding() {
        mVideoEncProcessor?.resumeProcess()
    }

    override fun pauseEncoding() {
        mVideoEncProcessor?.pauseProcess()
    }

    override fun offerEncode(data: ByteArray, timestamp: Long) {
        mEncodeHandler.post {
            val inIndex = mCodec?.dequeueInputBuffer(1000) ?: -1
            if (inIndex < 0) return@post
            var inputBuf: ByteBuffer? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                inputBuf = mCodec?.getInputBuffer(inIndex)
            } else {
                inputBuf = mCodec?.inputBuffers?.get(inIndex)
            }
            inputBuf?.apply {
                clear()
                put(data)
                mCodec?.queueInputBuffer(
                    inIndex,
                    0,
                    data.size,
                    System.nanoTime() / 1000L,
                    0
                )
            }
        }

    }

    override fun release() {
        stopEncoding()
        mVideoEncProcessor?.release()
        mCodec?.release()
        mEncodeThread.quitSafely()
        mEncodeHandler.removeCallbacksAndMessages(null)
        mVideoEncProcessor = null
        mCodec = null
        mListener = null
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun setInputSurface(surface: Surface) {
        mCodec?.setInputSurface(surface)
    }

    override fun getInputSurface(): Surface? {
        return mCodec?.createInputSurface()
    }


}