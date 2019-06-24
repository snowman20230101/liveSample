#include <jni.h>
#include "macro.h"

#include "librtmp/rtmp.h"
#include <x264.h>

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/avutil.h>
}

extern "C" JNIEXPORT jint JNICALL
Java_com_wwb_live_TestNdkHandler_getVersion(JNIEnv *env, jclass type) {
    jint version = avcodec_version();

    RTMP_Alloc();

    x264_picture_t *p = new x264_picture_t;

    LOGE("版本是：%d", version);
    return version;
}