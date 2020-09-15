package cn.com.ava.publish.configuration;

import androidx.annotation.IntDef;

/**
 * @Title: VideoConfiguration
 * @Package com.laifeng.sopcastsdk.configuration
 * @Description:
 * @Author Jim
 * @Date 16/9/14
 * @Time 下午3:20
 * @Version
 */
public final class VideoConfiguration implements IConfiguration {


    public static final int FORMAT_YUV420 = 1;
    public static final int FORMAT_NV21 = 2;
    public static final int FORMAT_ARGB = 3;
    public static final int FORMAT_SURFACE = 4;


    @IntDef({FORMAT_YUV420,FORMAT_NV21,FORMAT_ARGB,FORMAT_SURFACE})
    public @interface Format{}


    public static final int DEFAULT_HEIGHT = 720;
    public static final int DEFAULT_WIDTH = 1280;
    public static final int DEFAULT_FPS = 30;
    public static final int DEFAULT_MAX_BPS = 2000000;
    public static final int DEFAULT_MIN_BPS = 200;
    public static final int DEFAULT_IFI = 1;
    public static final String DEFAULT_MIME = "video/avc";

    public final int height;
    public final int width;
    public final int minBps;
    public final int maxBps;
    public final int fps;
    public final int ifi;
    public final String mime;
    public int previewWidth;
    public int previewHeight;
    public int format;

    private VideoConfiguration(final Builder builder) {
        height = builder.height;
        width = builder.width;
        minBps = builder.minBps;
        maxBps = builder.maxBps;
        fps = builder.fps;
        ifi = builder.ifi;
        mime = builder.mime;
        format = builder.format;
    }

    public int getPreviewWidth() {
        return previewWidth;
    }

    public void setPreviewWidth(int previewWidth) {
        this.previewWidth = previewWidth;
    }

    public int getPreviewHeight() {
        return previewHeight;
    }

    public void setPreviewHeight(int previewHeight) {
        this.previewHeight = previewHeight;
    }

    public static VideoConfiguration createDefault() {
        return new Builder().build();
    }

    public static class Builder {
        private int height = DEFAULT_HEIGHT;
        private int width = DEFAULT_WIDTH;
        private int minBps = DEFAULT_MIN_BPS;
        private int maxBps = DEFAULT_MAX_BPS;
        private int fps = DEFAULT_FPS;
        private int ifi = DEFAULT_IFI;
        private String mime = DEFAULT_MIME;
        private int format = FORMAT_YUV420;

        public Builder setSize(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder setBps(int minBps, int maxBps) {
            this.minBps = minBps;
            this.maxBps = maxBps;
            return this;
        }

        public Builder setFps(int fps) {
            this.fps = fps;
            return this;
        }

        public Builder setIfi(int ifi) {
            this.ifi = ifi;
            return this;
        }

        public Builder setMime(String mime) {
            this.mime = mime;
            return this;
        }

        public Builder setFormat(@Format int format){
            this.format = format;
            return this;
        }

        public VideoConfiguration build() {
            return new VideoConfiguration(this);
        }
    }
}
