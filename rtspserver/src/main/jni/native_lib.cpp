//
// Created by Administrator on 2020/9/1.
//

#include <jni.h>
#include <ByteStreamFileSource.hh>


//
//UsageEnvironment *usageEnv;
//FramedSource *videoSource;
//RTPSink *videoSink;
const char *inputString;


void play();
void afterPlaying(void* clientData);
//
//extern "C"
//JNIEXPORT jint JNICALL
//Java_cn_com_ava_rtspserver_RtspServerHelper_init(JNIEnv *env, jobject thiz, jstring input_file,
//
//   return -1;
//}



void play() {
    // Open the input file as a 'byte-stream file source':
    ByteStreamFileSource* fileSource = ByteStreamFileSource::createNew(*usageEnv,
                                                                       inputString);
    if (fileSource == NULL) {

    }
    FramedSource* videoES = fileSource;
    // Create a framer for the Video Elementary Stream:
    videoSource = H264VideoStreamFramer::createNew(*usageEnv, videoES);
    // Finally, start playing:
    videoSink->startPlaying(*videoSource, afterPlaying, videoSink);
}
//
//
//void afterPlaying(void* clientData){
//    videoSink->stopPlaying();
//    Medium::close(videoSource);
//    // Note that this also closes the input file that this source read from.
//
//    // Start playing once again:
//    play();
//}


