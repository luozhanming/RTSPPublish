package cn.com.ava.publish

import cn.com.ava.publish.audio.IAudioStreamController
import cn.com.ava.publish.configuration.AudioConfiguration
import cn.com.ava.publish.configuration.VideoConfiguration
import cn.com.ava.publish.servcie.RTSPServerService
import cn.com.ava.publish.video.IVideoStreamController

interface IRtspControl {

    var audioConfiguration:AudioConfiguration

    var videoConfiguration:VideoConfiguration

    var audioStreamController:IAudioStreamController?

    var videoStreamController:IVideoStreamController?

    var rtspService:RTSPServerService.RtspService?

    fun setMute(mute:Boolean)



}