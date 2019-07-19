#ifndef LIVESAMPLE_AUDIOCHANNEL_H
#define LIVESAMPLE_AUDIOCHANNEL_H


#include "librtmp/rtmp.h"
#include "faac/faac.h"
#include <sys/types.h>
#include <cstring>

#include "macro.h"

class AudioChannel {
    typedef void (*AudioCallBack)(RTMPPacket *packet);

public:
    AudioChannel();

    ~AudioChannel();

    void setAudioCallBack(AudioCallBack audioCallBack);

    void setAudioEncInfo(int samplesInHZ, int channels);

    int getInputSamples();

    void encodeData(int8_t *data);

    RTMPPacket *getAudioTag();

private:
    int mChannels;
    AudioCallBack audioCallBack;
    faacEncHandle audioCodec;
    unsigned long inputSamples;
    unsigned long maxOutputBytes;
    u_char *buffer = 0;
};

#endif //LIVESAMPLE_AUDIOCHANNEL_H
