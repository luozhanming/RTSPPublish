package cn.com.ava.rtspserver.elgsscreenrecoder.coder;

import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

import cn.com.ava.rtspserver.elgsscreenrecoder.glec.EGLRender;
import cn.com.ava.rtspserver.network.VideoFrame;


public class MediaEncoder extends Thread {
    private final String TAG = "oywf";

    private final String mime_type = MediaFormat.MIMETYPE_VIDEO_AVC;


    private DisplayManager displayManager;
    private MediaProjection projection;
    private MediaCodec mEncoder;
    private VirtualDisplay virtualDisplay;
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private EGLRender eglRender;
    private Surface surface;


    //屏幕相关
    private int screen_width;
    private int screen_height;
    private int screen_dpi;


    //编码参数相关
    private int frame_bit = 2000000;//2MB
    private int frame_rate = 20;//这里指的是Mediacodec30张图为1组 ，并不是视屏本身的FPS
    private int frame_internal = 1;//关键帧间隔 一组加一个关键帧
    private final int TIMEOUT_US = 10000;
    private int video_fps = 30;
    private byte[] sps = null;
    private byte[] pps = null;


    private onScreenCallBack onScreenCallBack;

    public void setOnScreenCallBack(MediaEncoder.onScreenCallBack onScreenCallBack) {
        this.onScreenCallBack = onScreenCallBack;
    }

    public interface onScreenCallBack {
        void onDataAviable(VideoFrame videoFrame);

        void onCutScreen(Bitmap bitmap);
    }

    public MediaEncoder(MediaProjection projection, int screen_width, int screen_height, int screen_dpi) {
        this.projection = projection;
        initScreenInfo(screen_width, screen_height, screen_dpi);
    }

    public MediaEncoder(DisplayManager displayManager, int screen_width, int screen_height, int screen_dpi) {
        this.displayManager = displayManager;
        initScreenInfo(screen_width, screen_height, screen_dpi);
    }

    private void initScreenInfo(int screen_width, int screen_height, int screen_dpi) {
        this.screen_width = screen_width;
        this.screen_height = screen_height;
        this.screen_dpi = screen_dpi;
    }

    /**
     * 设置视频FPS
     *
     * @param fps
     */
    public MediaEncoder setVideoFPS(int fps) {
        video_fps = fps;
        return this;
    }

    /**
     * 设置视屏编码采样率
     *
     * @param bit
     */
    public MediaEncoder setVideoBit(int bit) {
        frame_bit = bit;
        return this;
    }

    @Override
    public void run() {
        super.run();
        try {
            prepareEncoder();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (projection != null) {
            virtualDisplay = projection.createVirtualDisplay("screen", screen_width, screen_height, screen_dpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, eglRender.getDecodeSurface(), null, null);
        } else {
            virtualDisplay = displayManager.createVirtualDisplay("screen", screen_width, screen_height, screen_dpi,
                    eglRender.getDecodeSurface(), DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC);
        }
        startRecordScreen();
        release();
    }


    /**
     * 初始化编码器
     */
    private void prepareEncoder() throws IOException {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(mime_type, screen_width, screen_height);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, frame_bit);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frame_rate);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, frame_internal);
        mEncoder = MediaCodec.createEncoderByType(mime_type);
        mEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        surface = mEncoder.createInputSurface();
        eglRender = new EGLRender(surface, screen_width, screen_height, video_fps);
        eglRender.setCallBack(new EGLRender.onFrameCallBack() {
            @Override
            public void onUpdate() {
                startEncode();
            }

            @Override
            public void onCutScreen(Bitmap bitmap) {
                onScreenCallBack.onCutScreen(bitmap);
            }
        });
        mEncoder.start();
    }

    /**
     * 开始录屏
     */
    private void startRecordScreen() {
        eglRender.start();
        release();
    }

    private void startEncode() {
        ByteBuffer[] byteBuffers = null;
        int index = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_US);
        if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            resetOutputFormat();
        } else if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
            // Log.d("---", "retrieving buffers time out!");
            try {
                // wait 10ms
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        } else if (index >= 0) {
            encodeToVideoTrack(mEncoder.getOutputBuffer(index));
            mEncoder.releaseOutputBuffer(index, false);
        }
    }

    private void encodeToVideoTrack(ByteBuffer encodeData) {
        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
            mBufferInfo.size = 0;
        }
        if (mBufferInfo.size == 0) {
            Log.d(TAG, "info.size == 0, drop it.");
            encodeData = null;
        } else {
//            Log.d(TAG, "got buffer, info: size=" + mBufferInfo.size
//                    + ", presentationTimeUs=" + mBufferInfo.presentationTimeUs
//                    + ", offset=" + mBufferInfo.offset);
        }
        if (encodeData != null) {
            encodeData.position(mBufferInfo.offset);
            encodeData.limit(mBufferInfo.offset + mBufferInfo.size);
            byte[] bytes;
//            if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
//                //关键帧上添加sps,和pps信息
//                bytes = new byte[mBufferInfo.size + sps.length + pps.length];
//                System.arraycopy(sps, 0, bytes, 0, sps.length);
//                System.arraycopy(pps, 0, bytes, sps.length, pps.length);
//                encodeData.get(bytes, sps.length + pps.length, mBufferInfo.size);
//            } else {
            bytes = new byte[mBufferInfo.size];
            encodeData.get(bytes, 0, mBufferInfo.size);
            // }
            onScreenCallBack.onDataAviable(new VideoFrame(bytes, mBufferInfo.presentationTimeUs));
        }
    }

    private void resetOutputFormat() {
        MediaFormat newFormat = mEncoder.getOutputFormat();
        Log.i(TAG, "output format changed.\n new format: " + newFormat.toString());
        sendSpsPpsByteBuffer(newFormat);
    }


    /**
     * 获取编码SPS和PPS信息
     *
     * @param newFormat
     */
    private void sendSpsPpsByteBuffer(MediaFormat newFormat) {
        sps = newFormat.getByteBuffer("csd-0").array();
        pps = newFormat.getByteBuffer("csd-1").array();
        if (null != onScreenCallBack) {
            onScreenCallBack.onDataAviable(new VideoFrame(sps, "sps"));
            onScreenCallBack.onDataAviable(new VideoFrame(pps, "pps"));
        }
    }

    public void stopScreen() {
        if (eglRender != null) {
            eglRender.stop();
        }
        if (projection != null) {
            projection.stop();
        }
    }

    public void release() {
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
        }
    }

    public void cutScreen() {
        eglRender.cutScreen();
    }
}
