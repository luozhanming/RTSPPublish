package cn.com.ava.publish.util

import android.graphics.ImageFormat
import android.graphics.Rect
import android.media.*
import android.media.MediaCodecInfo.CodecCapabilities
import android.os.Build
import cn.com.ava.publish.configuration.AudioConfiguration
import cn.com.ava.publish.configuration.VideoConfiguration
import java.nio.ByteBuffer


object Utils {

    const val COLOR_FormatI420: Int = 1
    const val COLOR_FormatNV21 = 2
    const val COLOR_FormatARGB = 3

    fun getVideoMediaCodec(configuration: VideoConfiguration): MediaCodec? {
        val width = configuration.width
        val height = configuration.height
        val maxInputSize = width * height * 3 / 2 + 100
        val mediaFormat = MediaFormat.createVideoFormat(configuration.mime, width, height)
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxInputSize)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, configuration.maxBps)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, configuration.ifi)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, configuration.fps)

        mediaFormat.setInteger(
            MediaFormat.KEY_BITRATE_MODE,
            MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR
        )
        mediaFormat.setInteger(
            MediaFormat.KEY_COMPLEXITY,
            MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR
        )
        val format = configuration.format
        if(format==VideoConfiguration.FORMAT_YUV420){
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
        }else if(format==VideoConfiguration.FORMAT_NV21){
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
        }else if(format==VideoConfiguration.FORMAT_ARGB){
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatRGBAFlexible)
        }else{
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        }
        var encoder: MediaCodec? = null
        encoder = MediaCodec.createEncoderByType(configuration.mime)
        encoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        return encoder
    }


    fun getAudioMediaCodec(configuration: AudioConfiguration): MediaCodec {
        val audioSource = configuration.audioSource
        val smapleRateInHz = configuration.smapleRateInHz
        val format = configuration.format
        val channelConfig = configuration.channelConfig
        val bps = configuration.bps
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val mediaFormat = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            smapleRateInHz,
            if (configuration.channelConfig == AudioFormat.CHANNEL_IN_STEREO) 2 else 1
        )
        mediaFormat.setInteger(
            MediaFormat.KEY_AAC_PROFILE,
            MediaCodecInfo.CodecProfileLevel.AACObjectLC
        )
        val minBufferSize = AudioRecord.getMinBufferSize(smapleRateInHz, channelConfig, format)
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, minBufferSize + 100)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, configuration.bps)
        encoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        return encoder
    }


    fun chooseColorFormat(mime: String?): Int {
        var ci: MediaCodecInfo? = null
        val nbCodecs = MediaCodecList.getCodecCount()
        for (i in 0 until nbCodecs) {
            val mci = MediaCodecList.getCodecInfoAt(i)
            if (!mci.isEncoder) {
                continue
            }
            val types = mci.supportedTypes
            for (j in types.indices) {
                if (types[j].equals(mime, ignoreCase = true)) {
                    //Log.i(TAG, String.format("encoder %s types: %s", mci.getName(), types[j]));
                    ci = mci
                    break
                }
            }
        }
        var matchedColorFormat = 0
        val cc = ci!!.getCapabilitiesForType(mime)
        for (i in cc.colorFormats.indices) {
            val cf = cc.colorFormats[i]
            //Log.i(TAG, String.format("encoder %s supports color fomart %d", ci.getName(), cf));

            // choose YUV for h.264, prefer the bigger one.
            if (cf >= CodecCapabilities.COLOR_FormatYUV420Planar && cf <= CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
                if (cf > matchedColorFormat) {
                    matchedColorFormat = cf
                }
            }
        }

//        Log.i(TAG, String.format("encoder %s choose color format %d", ci.getName(), matchedColorFormat));
        return CodecCapabilities.COLOR_FormatYUV420Planar
    }


    fun isImageFormatSupported(image: Image): Boolean {
        val format: Int = image.getFormat()
        when (format) {
            ImageFormat.YUV_420_888, ImageFormat.NV21, ImageFormat.YV12 -> return true
        }
        return false
    }

    fun getDataFromImage(image: Image, colorFormat: Int): ByteArray {
        require(!(colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21)) { "only support COLOR_FormatI420 " + "and COLOR_FormatNV21" }
        if (!isImageFormatSupported(image)) {
            throw RuntimeException("can't convert Image to byte array, format " + image.getFormat())
        }
        val crop: Rect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            image.cropRect
        } else {
            Rect(0, 0, image.width, image.height)
        }
        val format: Int = image.getFormat()
        val width: Int = crop.width()
        val height: Int = crop.height()
        val planes: Array<Image.Plane> = image.getPlanes()
        val data = ByteArray(width * height * ImageFormat.getBitsPerPixel(format) / 8)
        val rowData = ByteArray(planes[0].getRowStride())

        var channelOffset = 0
        var outputStride = 1
        for (i in planes.indices) {
            when (i) {
                0 -> {
                    channelOffset = 0
                    outputStride = 1
                }
                1 -> if (colorFormat == COLOR_FormatI420) {
                    channelOffset = width * height
                    outputStride = 1
                } else if (colorFormat == COLOR_FormatNV21) {
                    channelOffset = width * height + 1
                    outputStride = 2
                }
                2 -> if (colorFormat == COLOR_FormatI420) {
                    channelOffset = (width * height * 1.25).toInt()
                    outputStride = 1
                } else if (colorFormat == COLOR_FormatNV21) {
                    channelOffset = width * height
                    outputStride = 2
                }
            }
            val buffer: ByteBuffer = planes[i].getBuffer()
            val rowStride: Int = planes[i].getRowStride()
            val pixelStride: Int = planes[i].getPixelStride()

            val shift = if (i == 0) 0 else 1
            val w = width shr shift
            val h = height shr shift
            buffer.position(rowStride * (crop.top shr shift) + pixelStride * (crop.left shr shift))
            for (row in 0 until h) {
                var length: Int
                if (pixelStride == 1 && outputStride == 1) {
                    length = w
                    buffer.get(data, channelOffset, length)
                    channelOffset += length
                } else {
                    length = (w - 1) * pixelStride + 1
                    buffer.get(rowData, 0, length)
                    for (col in 0 until w) {
                        data[channelOffset] = rowData[col * pixelStride]
                        channelOffset += outputStride
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length)
                }
            }
        }

        return data
    }


    /**
     * 添加ADTS头
     */
    fun addADTSHeader(
        byteBuffer: ByteBuffer,
        profile: Int,
        sampleRate: Int,
        channelCount: Int,
        packetLen: Int
    ) {
        if (packetLen <= 7) return
        val freqIdx: Int =
            getAudioFrequencyIndex(sampleRate) //
        val packet = ByteArray(7)
        // fill in ADTS data
        packet[0] = 0xFF.toByte()
        //        packet[1] = (byte) 0xF9;
        packet[1] = 0xF9.toByte() //支持在ios播放
        packet[2] = ((profile - 1 shl 6) + (freqIdx shl 2) + (channelCount shr 2)).toByte()
        packet[3] = ((channelCount and 3 shl 6) + (packetLen shr 11)).toByte()
        packet[4] = (packetLen and 0x7FF shr 3).toByte()
        packet[5] = ((packetLen and 7 shl 5) + 0x1F).toByte()
        packet[6] = 0xFC.toByte()
        byteBuffer.put(packet)
    }


    fun getAudioFrequencyIndex(frequency: Int): Int {
        var index = 0
        index = when (frequency) {
            96000 -> 0
            88200 -> 1
            64000 -> 2
            48000 -> 3
            44100 -> 4
            32000 -> 5
            24000 -> 6
            22050 -> 7
            16000 -> 8
            12000 -> 9
            11025 -> 10
            8000 -> 11
            7350 -> 12
            else -> 4
        }
        return index
    }
}