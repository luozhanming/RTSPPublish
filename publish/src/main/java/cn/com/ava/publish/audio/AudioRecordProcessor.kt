package cn.com.ava.publish.audio

import android.media.AudioRecord
import android.util.Log
import cn.com.ava.publish.configuration.AudioConfiguration
import java.util.*

/**
 * 音频采集处理
 * */
class AudioRecordProcessor(val configuration: AudioConfiguration) : Thread() {

    companion object {
        const val TAG = "AudioRecordProcessor"
    }

    private var mRecorder: AudioRecord

    private var mMinBufferSize: Int


    private var isRecording: Boolean = false

    private var isPause: Boolean = false

    private var mRecordBuffer: ByteArray


    private var isMute: Boolean = false


    private var mAudioRecListener: IAudioRecListener? = null


    init {
        mMinBufferSize = AudioRecord.getMinBufferSize(
            configuration.smapleRateInHz,
            configuration.channelConfig,
            configuration.format
        )
        mRecorder = AudioRecord(
            configuration.audioSource,
            configuration.smapleRateInHz,
            configuration.channelConfig,
            configuration.format,
            mMinBufferSize
        )
        mRecordBuffer = ByteArray(mMinBufferSize)
    }

    fun startRecording() {
        if (mRecorder.state == AudioRecord.STATE_INITIALIZED) {
            mRecorder.startRecording()
            start()
         //   audioTrack?.play()
        }
    }

    override fun run() {
        isRecording = true
        Log.d(TAG, "start recording.")
        while (isRecording) {
            while (isPause && isRecording) {
                sleep(100)
            }
            if (!isMute) {
                val ret = mRecorder.read(mRecordBuffer, 0, mMinBufferSize)
                if (ret >= 0) {
                //    audioTrack?.write(mRecordBuffer, 0, mMinBufferSize)
                    mAudioRecListener?.onData(mRecordBuffer, ret)
                } else {
                    mAudioRecListener?.onError(ret)
                    //采集出错
                }
            } else {
                Arrays.fill(mRecordBuffer, 0)
                mAudioRecListener?.onData(mRecordBuffer, mMinBufferSize)
            }

            sleep(2)
        }
    }

    fun stopRecording() {
        isRecording = false
        isPause = false
        this.join(100)
    }

    fun resumeRecording() {
        isPause = false
    }

    fun pause() {
        isPause = true
    }

    fun release() {
        stopRecording()
        if (mRecorder.state == AudioRecord.STATE_INITIALIZED) {
            mRecorder.stop()
        }
        this.mAudioRecListener = null
//        audioTrack?.release()
//        audioTrack = null
    }

    fun setListener(listener: IAudioRecListener) {
        this.mAudioRecListener = listener
    }

    fun setMute(value: Boolean) {
        this.isMute = value
    }

}