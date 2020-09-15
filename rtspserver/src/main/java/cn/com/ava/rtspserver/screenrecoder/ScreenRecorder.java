package cn.com.ava.rtspserver.screenrecoder;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.com.ava.rtspserver.network.VideoFrame;


public class ScreenRecorder extends Thread {
    private static final String TAG = "oywf";

    private int mWidth;
    private int mHeight;
    private int mBitRate;
    private int mDpi;
    private MediaProjection mMediaProjection;
    // parameters for the encoder
    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 20; // 30 fps
    private static final int IFRAME_INTERVAL = 2; // 10 seconds between I-frames
    private static final int TIMEOUT_US = 10000;

    private MediaCodec mEncoder;
    private ByteBuffer[] mOutputBuffers;

    private Surface mSurface;
    private AtomicBoolean mQuit = new AtomicBoolean(false);
    private VirtualDisplay mVirtualDisplay;
    private volatile ByteBuffer mSPS;
    private volatile ByteBuffer mPPS;
    private callBack callBack;

    public interface callBack {
        void onDataAviable(VideoFrame videoFrame);
    }


    public ScreenRecorder(int width, int height, int bitrate, int dpi, MediaProjection mp, callBack callBack) {
        super(TAG);
        mWidth = width;
        mHeight = height;
        mBitRate = bitrate;
        mDpi = dpi;
        mMediaProjection = mp;
        this.callBack = callBack;
    }


    public void setCallBack(ScreenRecorder.callBack callBack) {
        this.callBack = callBack;
    }

    public void requestSyncFrame() {
        Bundle bundle = new Bundle();
        bundle.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
        mEncoder.setParameters(bundle);
    }


    /**
     * stop task
     */
    public final void quit() {
        mQuit.set(true);
    }

    @Override
    public void run() {
        try {
            try {
                prepareEncoder();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG + "-display",
                    mWidth, mHeight, mDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    mSurface, null, null);
            Log.d(TAG, "created virtual display: " + mVirtualDisplay);
            recordVirtualDisplay();

        } finally {
            release();
        }
    }

    private void recordVirtualDisplay() {
        ByteBuffer outBuffer;
        while (!mQuit.get()) {
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int outputBufferId = mEncoder.dequeueOutputBuffer(info, TIMEOUT_US);
            if (outputBufferId >= 0) {
                // Output buffer is ready to be processed or rendered
                outBuffer = mOutputBuffers[outputBufferId];
                byte[] data = new byte[info.size];
                outBuffer.position(info.offset);
                outBuffer.limit(info.offset + info.size);
                outBuffer.get(data, info.offset, info.size);
                mEncoder.releaseOutputBuffer(outputBufferId, false);
                if (callBack != null) {
                   // Log.i("oywf", "rtsp--->" + info.presentationTimeUs);
                    callBack.onDataAviable(new VideoFrame(data, info.presentationTimeUs));
                }
            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                mOutputBuffers = mEncoder.getOutputBuffers();
               // Log.d(TAG, "output buffers changed");
            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // Subsequent data will conform to new format
                MediaFormat newFormat = mEncoder.getOutputFormat();
                mSPS = newFormat.getByteBuffer("csd-0");
                mPPS = newFormat.getByteBuffer("csd-1");
               // Log.i(TAG, "output format changed to " + newFormat.toString());
                if (callBack != null) {
                    if (mSPS != null && mPPS != null) {
                       // Log.i("oywf", "添加sps，pps");
                        callBack.onDataAviable(new VideoFrame(mSPS.array(), "sps"));
                        callBack.onDataAviable(new VideoFrame(mPPS.array(), "pps"));
                    }
                }
            }
        }
    }


    private void prepareEncoder() throws IOException {

        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

       // Log.d(TAG, "created video format: " + format);
        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mSurface = mEncoder.createInputSurface();
       // Log.d(TAG, "created input surface: " + mSurface);
        mEncoder.start();
        mOutputBuffers = mEncoder.getOutputBuffers();
    }

    private void release() {
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
        }
    }
}
