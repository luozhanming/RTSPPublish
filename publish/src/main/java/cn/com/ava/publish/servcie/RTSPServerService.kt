package cn.com.ava.publish.servcie

import android.app.Service
import android.content.Intent
import android.os.IBinder
import cn.com.ava.publish.IRtspService
import cn.com.ava.rtspserver.network.AudioData
import cn.com.ava.rtspserver.network.StreamServer
import cn.com.ava.rtspserver.network.VideoFrame

class RTSPServerService: Service() {


    private var mServer:StreamServer?=null


    override fun onCreate() {
        mServer = StreamServer(this, 9002).apply {
            setWiFiAvailable(true)
            setH264Available(true)
            setAudioAvailable(true)
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        return RtspService()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
        mServer?.close()
        mServer = null
    }

    inner class RtspService:IRtspService.Stub(){
        override fun pushAudioConfig(config: ByteArray?) {
            val data = AudioData(config)
            mServer?.push(data)
        }

        override fun pushVideoData(data: ByteArray?, timestamp: Long) {
            val data = VideoFrame(data,timestamp)
            mServer?.push(data)
        }

        override fun pushAudioData(data: ByteArray?, timestamp: Long) {
            val data = AudioData(data,timestamp)
            mServer?.push(data)
        }

        override fun pushPps(pps: ByteArray?) {
            val data = VideoFrame(pps,"pps")
            mServer?.push(data)
        }

        override fun pushSps(sps: ByteArray?) {
            val data = VideoFrame(sps,"sps")
            mServer?.push(data)
        }

    }
}