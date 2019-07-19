#ifndef LIVESAMPLE_VIDEOCHANNL_H
#define LIVESAMPLE_VIDEOCHANNL_H


#include "librtmp/rtmp.h"
#include <pthread.h>
#include "x264/x264.h"
#include "macro.h"

class VideoChannel {
    typedef void (*VideoCallBack)(RTMPPacket* packet);

public:
    // 定义构造函数
    VideoChannel();
    //定义析构函数
    ~VideoChannel();

    void setVideoCallBack(VideoCallBack videoCallBack);

    //创建x264编码器
    void setVideoEncInfo(int width, int height, int fps, int bitrate);

    void encodeData(int8_t *data);

private:
    pthread_mutex_t mutex;

    VideoCallBack videoCallBack;

    int mWidth;
    int mHeight;
    int mFps;
    int mBitrate;

    x264_t *videoCodec = 0;
    x264_picture_t *pic_in = 0;

    int ySize;
    int uvSize;

    void sendSpsPps(uint8_t *sps, uint8_t *pps, int sps_len, int pps_len);
    void sendFrame(int type, uint8_t *payload, int i_payload);
};


#endif //LIVESAMPLE_VIDEOCHANNL_H
