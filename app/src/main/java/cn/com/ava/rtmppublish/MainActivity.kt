package cn.com.ava.rtmppublish

import android.Manifest
import android.app.Activity
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import cn.com.ava.publish.CameraLivingView
import cn.com.ava.publish.IRtspService
import cn.com.ava.publish.ScreenRecordService
import cn.com.ava.publish.servcie.RTSPServerService

class MainActivity : AppCompatActivity() {

    lateinit var cameraLivingView: CameraLivingView

    private val startRtspServiceIntent: Intent by lazy {
        Intent(this, RTSPServerService::class.java)
    }

    private val startScreenRecordIntent: Intent by lazy {
        Intent(this, ScreenRecordService::class.java)
    }

    private var mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            mRtspService = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mRtspService = IRtspService.Stub.asInterface(service) as RTSPServerService.RtspService
            cameraLivingView.rtspService = mRtspService
        }
    }

    private var mRtspService: RTSPServerService.RtspService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            1
        )
//        EncodeAndMuxTest().testEncodeVideoToMp4()
        //    CameraToMpegTest().testEncodeCameraToMp4()
       setContentView(R.layout.activity_main)
        cameraLivingView = findViewById(R.id.cameraview)
        cameraLivingView.setLifecycleOwner(this)
        startService(startRtspServiceIntent)
        bindService(startRtspServiceIntent, mConnection, Service.BIND_AUTO_CREATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val mps = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val intent = mps.createScreenCaptureIntent()
            startActivityForResult(intent, 1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            startScreenRecordIntent.putExtras(data!!)
            startService(startScreenRecordIntent)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

    }


    override fun onDestroy() {
        super.onDestroy()
        unbindService(mConnection)
        stopService(startRtspServiceIntent)
    }
}