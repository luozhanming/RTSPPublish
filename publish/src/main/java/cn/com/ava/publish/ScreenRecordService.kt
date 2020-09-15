package cn.com.ava.publish

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.AudioRecord
import android.media.ImageReader
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import android.view.Surface
import androidx.annotation.RequiresApi
import cn.com.ava.publish.audio.AndroidAudioStreamController
import cn.com.ava.publish.audio.IAudioEncodeListener
import cn.com.ava.publish.audio.IAudioStreamController
import cn.com.ava.publish.configuration.AudioConfiguration
import cn.com.ava.publish.configuration.VideoConfiguration
import cn.com.ava.publish.servcie.RTSPServerService
import cn.com.ava.publish.video.IVEncListener
import cn.com.ava.publish.video.IVideoStreamController
import cn.com.ava.publish.video.VideoStreamController
import cn.com.ava.rtspserver.network.AudioData
import cn.com.ava.rtspserver.network.StreamServer
import cn.com.ava.rtspserver.network.VideoFrame

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ScreenRecordService : Service(), IRtspControl, IVEncListener, IAudioEncodeListener {
    
    companion object{
        const val TAG = "ScreenRecordService"
    }


    private var mMediaProjection: MediaProjection? = null

    private var mScreenReader: ImageReader? = null

    private val mHandlerThread: HandlerThread by lazy {
        HandlerThread("ScreenRecord").apply { start() }
    }

    private val mThreadHandler: Handler by lazy {
        Handler(mHandlerThread.looper)
    }

    private var mSurface:Surface?=null

    private var mRtspServer:StreamServer?=null


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    override fun onCreate() {
        super.onCreate()
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        val dpi = resources.displayMetrics.densityDpi
        mScreenReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3)
        videoStreamController = VideoStreamController(videoConfiguration)
        audioStreamController = AndroidAudioStreamController(audioConfiguration)
        videoStreamController?.mListener = this
        mSurface =   videoStreamController?.getInputSurface()
        audioStreamController?.setListener(this)
        videoStreamController?.startEncoding()
        audioStreamController?.startRecording()
        mRtspServer = StreamServer(this, 9003).apply {
            setWiFiAvailable(true)
            setH264Available(true)
            setAudioAvailable(true)
        }
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        val dpi = resources.displayMetrics.density.toInt()
        val mediaProjectionManager: MediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mMediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, intent)
        mMediaProjection?.createVirtualDisplay(
            "ScreenRecord", videoConfiguration.width, videoConfiguration.height, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mSurface, null, null
        )
        return START_NOT_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
        videoStreamController?.mListener = null
        audioStreamController?.setListener(null)
        videoStreamController?.startEncoding()
        audioStreamController?.stopRecording()
        videoStreamController?.release()
        audioStreamController?.release()
        mRtspServer?.close()
        mRtspServer=null
        videoStreamController = null
        audioStreamController = null
    }

    //region IVEncListener

    override fun onVData(data: ByteArray, isKeyFrame: Boolean, presentationTime: Long) {
        Log.i(TAG, "onVData: isKeyFrame=>$isKeyFrame;presentationTime=>${presentationTime}")
        val frame = VideoFrame(data,presentationTime)
        mRtspServer?.push(frame)
    }

    override fun onVideoError() {
    }

    override fun onSps(sps: ByteArray) {
        Log.i(TAG, "onSps: ")
        val frame = VideoFrame(sps,"sps")
        mRtspServer?.push(frame)
    }

    override fun onPps(pps: ByteArray) {
        Log.i(TAG, "onPps: ")
        val frame = VideoFrame(pps,"pps")
        mRtspServer?.push(frame)
    }

    //endregion


    //region IAEncListener
    override fun onAData(data: ByteArray, size: Int, offset: Int, timestamp: Long) {
        Log.i(TAG, "onAData: ")
        val data = AudioData(data,timestamp)
        mRtspServer?.push(data)
    }

    override fun onAudioError(err: Int) {
        Log.i(TAG, "onAudioError: ")
    }

    override fun onAudioConfigure(configure: ByteArray) {
        Log.i(TAG, "onAudioConfigure: ")
        val data = AudioData(configure)
        mRtspServer?.push(data)
    }

    //endregion

    //region IRtspControl

    override var audioConfiguration: AudioConfiguration = AudioConfiguration.Builder().audioSource(MediaRecorder.AudioSource.REMOTE_SUBMIX).build()
        get() = field
    override var videoConfiguration: VideoConfiguration = VideoConfiguration.Builder().setSize(1280,720)
        .setFormat(VideoConfiguration.FORMAT_SURFACE)
        .setBps(0,4_000_000)
        .build()
        get() = field


    override var audioStreamController: IAudioStreamController? = null
        get() = field

    override var videoStreamController: IVideoStreamController? = null
        get() = field

    override var rtspService: RTSPServerService.RtspService? = null
        get() = field
        set(value) {
            field = value
        }

    override fun setMute(mute: Boolean) {
        audioStreamController?.isMute = mute
    }


    //endregion


}