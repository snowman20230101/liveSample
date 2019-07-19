package com.wwb.live;

import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private LivePusher mLivePusher;
    private SurfaceView surfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        surfaceView = findViewById(R.id.surfaceView);
        mLivePusher = new LivePusher(this, 800, 480, 800_000, 10, Camera.CameraInfo.CAMERA_FACING_BACK);
//        mLivePusher = new LivePusher(this, 720, 1280, 800_000, 10, Camera.CameraInfo.CAMERA_FACING_BACK);
        //  设置摄像头预览的界面
        mLivePusher.setPreviewDisplay(surfaceView.getHolder());
    }

    public void switchCamera(View view) {
        mLivePusher.switchCamera();
    }


    public void startLive(View view) {
        mLivePusher.startLive("rtmp://42.56.89.84:1935/myapp/mystream");
    }

    public void stopLive(View view) {
        mLivePusher.stopLive();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLivePusher.release();
    }
}
