#include <jni.h>
#include "macro.h"

#include "VideoChannel.h"
#include "AudioChannel.h"
#include "safe_queue.h"
#include "lame/lame.h"

// 声明队列
SafeQueue<RTMPPacket *> packets;
VideoChannel *videoChannel = 0;
AudioChannel *audioChannel = 0;

int isStart = 0;
pthread_t pid;
int readyPushing = 0;

uint32_t start_time;

/**
 * 视频编码器回调
 * @param packet
 */
void callback(RTMPPacket *packet) {
    if (packet) {
        //设置时间戳
        packet->m_nTimeStamp = RTMP_GetTime() - start_time;
        packets.push(packet);
    }
}

/**
 * 队列回调
 * @param packet
 */
void releaseCallBack(RTMPPacket *&packet) {
    if (packet) {
        RTMPPacket_Free(packet);
        DELETE(packet);
        packet = 0;
    }
}

/**
 * 开始直播，线程回调
 * @param args
 * @return
 */
void *start(void *args) {
    char *url = static_cast<char *>(args);
    RTMP *rtmp = 0;
    do {
        rtmp = RTMP_Alloc();
        if (!rtmp) {
            LOGE("alloc rtmp failed!");
            break;
        }

        RTMP_Init(rtmp);
        int ret = RTMP_SetupURL(rtmp, url);
        if (!ret) {
            LOGE("设置地址失败:%s", url);
            break;
        }

        //超时时间
        rtmp->Link.timeout = 5;
        RTMP_EnableWrite(rtmp);
        ret = RTMP_Connect(rtmp, 0);
        if (!ret) {
            LOGE("连接服务器:%s", url);
            break;
        }

        ret = RTMP_ConnectStream(rtmp, 0);
        if (!ret) {
            LOGE("连接流:%s", url);
            break;
        }

        start_time = RTMP_GetTime();
        // 表示可以推流了。
        readyPushing = 1;
        packets.setWork(1);
        // 保证第一个数据是 aac解码数据包
        callback(audioChannel->getAudioTag());

        RTMPPacket *packet = 0;
        while (readyPushing) {
            packets.pop(packet);
            if (!isStart) {
                break;
            }

            if (!packet) {
                continue;
            }

            packet->m_nInfoField2 = rtmp->m_stream_id;
            ret = RTMP_SendPacket(rtmp, packet, 1);
            releaseCallBack(packet);
            if (!ret) {
                LOGE("发送失败 ret=%d", ret);
                break;
            }
        }

        releaseCallBack(packet);
    } while (0);

    isStart = 0;
    readyPushing = 0;
    packets.setWork(0);
    packets.clear();
    if (!rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
    }

    delete (url);

    return 0;
}

void printLameLog(){
    const char* lameVersion = get_lame_version();
    LOGE("lame version is %s", lameVersion);
}

/**
 * 初始化
 */
extern "C" JNIEXPORT void JNICALL
Java_com_wwb_live_LivePusher_native_1init(JNIEnv *env, jobject instance) {
    // 准备一个Video编码器的工具类 ：进行编码
    videoChannel = new VideoChannel;
    videoChannel->setVideoCallBack(callback);
    audioChannel = new AudioChannel;
    audioChannel->setAudioCallBack(callback);

    // 测试
    printLameLog();

    // 准备一个队列, 打包好的数据 放入队列，在线程中统一的取出数据再发送给服务器
    packets.setReleaseCallback(releaseCallBack);
}

/**
 * 开始直播
 */
extern "C" JNIEXPORT void JNICALL
Java_com_wwb_live_LivePusher_native_1start(JNIEnv *env, jobject instance, jstring path_) {
    if (isStart)
        return;

    isStart = 1;
    const char *path = env->GetStringUTFChars(path_, 0);
    char *url = new char[strlen(path + 1)];
    strcpy(url, path);
    pthread_create(&pid, 0, start, url);
    env->ReleaseStringUTFChars(path_, path);
}

/**
 *  创建设置视频编码器
 */
extern "C" JNIEXPORT void JNICALL
Java_com_wwb_live_LivePusher_native_1setVideoEncInfo(JNIEnv *env, jobject instance, jint width,
                                                     jint height, jint fps, jint bitrate) {
    if (videoChannel) {
        videoChannel->setVideoEncInfo(width, height, fps, bitrate);
    }
}

/**
 *  推视频流
 */
extern "C" JNIEXPORT void JNICALL
Java_com_wwb_live_LivePusher_native_1pushVideo(JNIEnv *env, jobject instance, jbyteArray data_) {
    if (!videoChannel || !readyPushing) {
        return;
    }

    jbyte *data = env->GetByteArrayElements(data_, NULL);
    videoChannel->encodeData(data);
    env->ReleaseByteArrayElements(data_, data, 0);
}

/**
 *  停止直播
 */
extern "C" JNIEXPORT void JNICALL
Java_com_wwb_live_LivePusher_native_1stop(JNIEnv *env, jobject instance) {
    readyPushing = 0;
    packets.setWork(0);
    pthread_join(pid, 0);
}

/**
 * 释放资源
 */
extern "C" JNIEXPORT void JNICALL
Java_com_wwb_live_LivePusher_native_1release(JNIEnv *env, jobject instance) {
    DELETE(videoChannel);
    DELETE(audioChannel);
}

/**
 * 创建设置音频编码器
 */
extern "C" JNIEXPORT void JNICALL
Java_com_wwb_live_LivePusher_native_1setAudioEncInfo(JNIEnv *env, jobject instance, jint sampleINHZ,
                                                     jint channels) {
    if (audioChannel) {
        audioChannel->setAudioEncInfo(sampleINHZ, channels);
    }
}

/**
 *
 */
extern "C" JNIEXPORT jint JNICALL
Java_com_wwb_live_LivePusher_getInputSamples(JNIEnv *env, jobject instance) {
    if (audioChannel) {
        return audioChannel->getInputSamples();
    }

    return -1;
}

/**
 * 音频推流
 */
extern "C" JNIEXPORT void JNICALL
Java_com_wwb_live_LivePusher_native_1pushAudio(JNIEnv *env, jobject instance, jbyteArray data_) {
    if (!audioChannel || !readyPushing) {
        return;
    }
    jbyte *data = env->GetByteArrayElements(data_, NULL);
    audioChannel->encodeData(data);
    env->ReleaseByteArrayElements(data_, data, 0);
}