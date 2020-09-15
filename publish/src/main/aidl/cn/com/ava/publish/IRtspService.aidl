// IRtspService.aidl
package cn.com.ava.publish;

// Declare any non-default types here with import statements

interface IRtspService {


void pushAudioConfig(in byte[] config);

void pushAudioData(in byte[] data,long timestamp);

void pushVideoData(in byte[] data,long timestamp);

void pushPps(in byte[] pps);

void pushSps(in byte[] sps);
}
