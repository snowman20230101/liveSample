package com.wwb.live;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioChannel {

    private int inputSamples;
    private ExecutorService executor;
    private AudioRecord audioRecord;
    private LivePusher mLivePusher;
    private int channels = 2;
    private boolean isLiving;
    private Activity mActivity;

    public AudioChannel(LivePusher livePusher, Activity activity) {
        mLivePusher = livePusher;
        mActivity = activity;
        executor = Executors.newSingleThreadExecutor();
        //准备录音机 采集pcm 数据
        int channelConfig;
        if (channels == 2) {
            channelConfig = AudioFormat.CHANNEL_IN_STEREO;
        } else {
            channelConfig = AudioFormat.CHANNEL_IN_MONO;
        }

        audioSpeakerPhone();

        mLivePusher.native_setAudioEncInfo(44100, channels);
        //16 位 2个字节
        inputSamples = mLivePusher.getInputSamples() * 2;
        //最小需要的缓冲区
        int minBufferSize = AudioRecord.getMinBufferSize(44100, channelConfig, AudioFormat.ENCODING_PCM_16BIT) * 2;
        //1、麦克风 2、采样率 3、声道数 4、采样位
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, channelConfig, AudioFormat.ENCODING_PCM_16BIT, minBufferSize > inputSamples ? minBufferSize : inputSamples);
    }

    private void audioSpeakerPhone(){
        AudioManager audioManager = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setSpeakerphoneOn(false);
        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, 0, AudioManager.STREAM_VOICE_CALL);
        audioManager.setMode(AudioManager.MODE_IN_CALL);
    }

    public void startLive() {
        isLiving = true;
        executor.submit(new AudioTask());
    }

    public void stopLive() {
        isLiving = false;
    }

    public void release() {
        audioRecord.release();
    }

    class AudioTask implements Runnable {

        @Override
        public void run() {
            //启动录音机
            audioRecord.startRecording();
            byte[] bytes = new byte[inputSamples];
            while (isLiving) {
                int len = audioRecord.read(bytes, 0, bytes.length);
                if (len > 0) {
                    //送去编码
                    mLivePusher.native_pushAudio(bytes);
                }
            }
            //停止录音机
            audioRecord.stop();
        }
    }
}
