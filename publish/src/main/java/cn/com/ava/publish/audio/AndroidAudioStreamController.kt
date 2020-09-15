package cn.com.ava.publish.audio

import android.util.Log
import cn.com.ava.publish.configuration.AudioConfiguration

class AndroidAudioStreamController(val configuration: AudioConfiguration) : IAudioStreamController {

    companion object {
        const val TAG = "AndroidAudioRecorder"
    }


    var mRecorderProcessor: AudioRecordProcessor? = null

    var mAudioEncoder: AudioEncoder? = null

    var mListener: IAudioEncodeListener? = null


    var mAudioRecListener: IAudioRecListener = object : IAudioRecListener {
        override fun onData(data: ByteArray, size: Int) {
            Log.d(TAG, "on PCM Data=>size:${size};")
            mAudioEncoder?.offerEncode(data, size)
        }

        override fun onError(err: Int) {
            Log.d(TAG, "on Error=>error:${err};")
        }
    }

    var mAudioEncListener: IAudioEncodeListener = object : IAudioEncodeListener {
        override fun onAData(data: ByteArray, size: Int, offset: Int, timestamp: Long) {
            Log.d(TAG, "on Encode Data=>size:${size};")
            if(!isMute){
                mListener?.onAData(data, size, offset, timestamp)
            }else{
                val muteData = ByteArray(size)
                muteData.fill(0)
                mListener?.onAData(muteData,size,offset,timestamp)
            }

        }


        override fun onAudioError(err: Int) {
        }

        override fun onAudioConfigure(configure: ByteArray) {
            mListener?.onAudioConfigure(configure)
        }
    }

    override var isMute: Boolean = false
        get() = field
        set(value) {
            field = value
        }

    init {
        mRecorderProcessor = AudioRecordProcessor(configuration)
        mRecorderProcessor?.setListener(mAudioRecListener)
        mAudioEncoder = AudioEncoder(configuration)
        mAudioEncoder?.setListener(mAudioEncListener)
    }


    override fun startRecording() {
        mRecorderProcessor?.startRecording()
        mAudioEncoder?.prepareEncode()
    }

    override fun stopRecording() {
        mRecorderProcessor?.stopRecording()
        mAudioEncoder?.stop()
    }

    override fun resume() {
        mRecorderProcessor?.resumeRecording()
    }

    override fun pause() {
        mRecorderProcessor?.pause()
    }

    override fun release() {
        mRecorderProcessor?.release()
        mAudioEncoder?.release()
        mRecorderProcessor = null
        mAudioEncoder = null
        mListener = null
    }

    override fun setListener(listener: IAudioEncodeListener?) {
        mListener = listener
    }


}