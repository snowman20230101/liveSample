#ifndef LIVESAMPLE_MACRO_H
#define LIVESAMPLE_MACRO_H

#include <android/log.h>

/**
 * 定义日志输出
 */
#define TAG "LiveSample"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

#endif //LIVESAMPLE_MACRO_H
