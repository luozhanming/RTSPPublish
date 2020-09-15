package cn.com.ava.publish

import android.content.Context
import android.media.Image
import android.util.AttributeSet
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import cn.com.ava.publish.audio.AndroidAudioStreamController
import cn.com.ava.publish.audio.IAudioEncodeListener
import cn.com.ava.publish.audio.IAudioStreamController
import cn.com.ava.publish.configuration.AudioConfiguration
import cn.com.ava.publish.configuration.VideoConfiguration
import cn.com.ava.publish.servcie.RTSPServerService
import cn.com.ava.publish.util.Utils
import cn.com.ava.publish.video.IVEncListener
import cn.com.ava.publish.video.IVideoStreamController
import cn.com.ava.publish.video.VideoStreamController
import cn.com.ava.rtspserver.network.AudioData
import cn.com.ava.rtspserver.network.StreamServer
import cn.com.ava.rtspserver.network.VideoFrame
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.controls.Mode
import com.otaliastudios.cameraview.frame.Frame
import com.otaliastudios.cameraview.frame.FrameProcessor
import com.otaliastudios.cameraview.size.Size
import com.otaliastudios.cameraview.size.SizeSelector
import kotlin.math.abs

class CameraLivingView(context: Context, attrs: AttributeSet) : CameraView(context, attrs),
    FrameProcessor, IRtspControl {

    companion object {
        const val TAG = "CameraLivingView"
    }

    //region Members

    //音视频采集编码
   // private var mRtspServer: StreamServer? = null
    private var hasInitEncode: Boolean = false

    //计算帧率
    private var frameCount: Int = 0
    private var lastTime: Long = 0


    //视频编码结果回调
    private var mVideoEncListener: IVEncListener = object : IVEncListener {
        override fun onVData(data: ByteArray, isKeyFrame: Boolean, presentationTime: Long) {
            Log.d(
                TAG,
                "onData Video: dataSize=>${data.size};presentationTime=>$presentationTime"
            )
            frameCount++;
            var curTime = System.currentTimeMillis()
            if (curTime - lastTime >= 1000) {
                Log.d(TAG, "fps:$frameCount")
                frameCount = 0
                lastTime = curTime
            } else {
                frameCount++
            }
            val frame = VideoFrame(data, presentationTime)
            rtspService?.pushVideoData(data,presentationTime)
          //  mRtspServer?.push(frame)
        }

        override fun onVideoError() {

        }

        override fun onSps(sps: ByteArray) {
            rtspService?.pushSps(sps)
        }

        override fun onPps(pps: ByteArray) {
            rtspService?.pushPps(pps)
        }
    }

    private var mAudioEncListener: IAudioEncodeListener = object : IAudioEncodeListener {
        override fun onAData(data: ByteArray, size: Int, offset: Int, timestamp: Long) {
            Log.d(TAG, "onData Audio: dataSize=>${size};timestamp=>${timestamp}")
            rtspService?.pushAudioData(data, timestamp)
        }

        override fun onAudioError(err: Int) {

        }

        override fun onAudioConfigure(configure: ByteArray) {
            rtspService?.pushAudioConfig(configure)
        }
    }

    //endregion


    init {
        addFrameProcessor(this)
        mode = Mode.PICTURE
        frameProcessingMaxWidth = 1920
        frameProcessingMaxHeight = 1080
        previewFrameRate = 30f
        facing = Facing.BACK
    }

    //region Lifecycle

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun updateConfiguration() {
        selectExpectedPreviewSize()
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun release() {
     //   mRtspServer?.close()
        videoStreamController?.release()
        audioStreamController?.release()
        removeFrameProcessor(this)
        videoStreamController?.mListener = null
        audioStreamController?.setListener(null)
       // mRtspServer = null

    }

    //endregion


    //region IRtspControl

    override var audioConfiguration: AudioConfiguration = AudioConfiguration.Builder().build()
        get() = field
        set(value) {
            field = value
        }
    override var videoConfiguration: VideoConfiguration = VideoConfiguration.Builder().setSize(1920,1080).setBps(0,4_000_000).build()
        get() = field
        set(value) {
            field = value
        }


    override var audioStreamController: IAudioStreamController? = null
        get() = field
        set(value) {
            field = value
        }
    override var videoStreamController: IVideoStreamController? = null
        get() = field
        set(value) {
            field = value
        }
    override var rtspService: RTSPServerService.RtspService? = null
        get() = field
        set(value) {
            field = value
        }

    override fun setMute(mute: Boolean) {
        audioStreamController?.isMute = mute
    }

    //endregion

    override fun process(frame: Frame) {
        val time = frame.time
        val size = frame.size
        val format = frame.format
        val rotationToUser = frame.rotationToUser
        val rotationToView = frame.rotationToView
        if (frame.dataClass == ByteArray::class.java) {//Camera1处理
            val data = frame.getData<ByteArray>()
        } else if (frame.dataClass == Image::class.java) {  //Camera2处理
            val data = frame.getData<Image>()
            val yuvSize = data.planes.size
            val y = data.planes[0]
            val u = data.planes[1]
            val v = data.planes[2]
            val dataFromImage = Utils.getDataFromImage(data, Utils.COLOR_FormatI420)
            videoStreamController?.offerEncode(dataFromImage, data.timestamp)
        }
    }



    //根据选择最合适的尺寸
    private fun selectExpectedPreviewSize() {
        Log.d(TAG, "selectExpectedPreviewSize: ")
        val config = videoConfiguration
        val width = config.width
        val height = config.height
        val selector: SizeSelector = SizeSelector {
            var minDiff = 0
            var selectSizeIndex = 0
            //通过标准差找出最接近的一个
            it.forEachIndexed { i, size ->
                val width1 = size.width
                val height1 = size.height
                val diff1 =
                    width1 * width1 * 2 + height1 * height1 * 9
                val diff2 = width * width * 2 + height * height * 9
                val diff = abs(diff1 - diff2)
                if (i == 0) {
                    minDiff = diff
                    selectSizeIndex = i
                } else {
                    if (diff <= minDiff) {
                        minDiff = diff
                        selectSizeIndex = i
                    }
                }
            }
            val removeAt = it.removeAt(selectSizeIndex)
            it.add(0, removeAt)
            videoConfiguration.setPreviewWidth(removeAt.width)
            videoConfiguration.setPreviewHeight(removeAt.height)
            if (!hasInitEncode) {
                initEncoders()
                hasInitEncode = true
            }
            Log.d(TAG, "Use previewsize:${removeAt}")
            return@SizeSelector arrayListOf<Size>(removeAt)
        }
        setPreviewStreamSize(selector)
    }


    //初始化音视频采集编码器
    private fun initEncoders() {
        Log.d(TAG, "initEncoders: ")
        audioStreamController = AndroidAudioStreamController(audioConfiguration)
        videoStreamController = VideoStreamController(videoConfiguration)
        videoStreamController?.startEncoding()
        audioStreamController?.startRecording()
        videoStreamController?.mListener = mVideoEncListener
        audioStreamController?.setListener(mAudioEncListener)


    }


}