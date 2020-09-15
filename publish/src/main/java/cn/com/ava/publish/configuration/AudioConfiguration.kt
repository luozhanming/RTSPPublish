package cn.com.ava.publish.configuration

import android.media.AudioFormat
import android.media.MediaRecorder

class AudioConfiguration private constructor(
    val audioSource: Int,
    val smapleRateInHz: Int,
    val channelConfig: Int,
    val format: Int,
    val bps: Int
) {

    class Builder {

        private var audioSource: Int = MediaRecorder.AudioSource.VOICE_COMMUNICATION

        private var sampleRateInHz: Int = 44100

        private var channelConfig: Int = AudioFormat.CHANNEL_IN_STEREO

        private var audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT

        private var bps: Int = 320_000

        fun audioSource(source: Int): Builder {
            this.audioSource = source
            return this
        }

        fun sampleRateInHz(rate: Int): Builder {
            this.audioSource = rate
            return this
        }

        fun channelConfig(channel: Int): Builder {
            this.audioSource = channel
            return this
        }

        fun audioFormat(format: Int): Builder {
            this.audioSource = format
            return this
        }

        fun bitRate(bps: Int): Builder {
            this.bps = bps
            return this
        }


        fun build(): AudioConfiguration {
            return AudioConfiguration(
                audioSource,
                sampleRateInHz,
                channelConfig,
                audioFormat,
                bps
            );
        }


    }
}