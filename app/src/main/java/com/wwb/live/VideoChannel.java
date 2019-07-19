package com.wwb.live;

import android.app.Activity;
import android.hardware.Camera;
import android.view.SurfaceHolder;

public class VideoChannel implements Camera.PreviewCallback, CameraHelper.OnChangedSizeListener {

    private final static String TAG = VideoChannel.class.getSimpleName();

    private LivePusher mLivePusher;
    private CameraHelper cameraHelper;
    private int mBitrate;
    private int mFps;
    private boolean isLiving;

    public VideoChannel(LivePusher livePusher, Activity activity, int width, int height, int bitrate, int fps, int cameraId) {
        mLivePusher = livePusher;
        mBitrate = bitrate;
        mFps = fps;
        cameraHelper = new CameraHelper(activity, cameraId, width, height);
        //1、让camerahelper的
        cameraHelper.setPreviewCallback(this);
        //2、回调 真实的摄像头数据宽、高
        cameraHelper.setOnChangedSizeListener(this);
    }

    public void startLive() {
        isLiving = true;
    }

    public void stopLive() {
        isLiving = false;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (isLiving) {
            mLivePusher.native_pushVideo(data);
        }
    }

    /**
     * 真实摄像头数据宽、高
     *
     * @param w
     * @param h
     */
    @Override
    public void onChanged(int w, int h) {
        mLivePusher.native_setVideoEncInfo(w, h, mFps, mBitrate);
    }

    public void setPreviewDisplay(SurfaceHolder surfaceHolder) {
        cameraHelper.setPreviewDisplay(surfaceHolder);
    }

    public void switchCamera() {
        cameraHelper.switchCamera();
    }
}
