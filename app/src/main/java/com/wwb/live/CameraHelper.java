package com.wwb.live;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * 相机管理
 */
public class CameraHelper implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private final static String TAG = CameraHelper.class.getSimpleName();

    private Activity mActivity;
    private int mHeight;
    private int mWidth;
    private int mCameraId;
    private Camera mCamera;

    private byte[] buffer;
    private SurfaceHolder mSurfaceHolder;
    private Camera.PreviewCallback mPreviewCallback;
    private OnChangedSizeListener mOnChangedSizeListener;

    byte[] bytes;
    private int mRotation;

    public CameraHelper(Activity activity, int cameraId, int width, int height) {
        mActivity = activity;
        mCameraId = cameraId;
        mWidth = width;
        mHeight = height;
    }

    /**
     * 切换摄像头
     */
    public void switchCamera() {
        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }

        stopPreview();
        startPreview();
    }

    public void setPreviewDisplay(SurfaceHolder surfaceHolder) {
        mSurfaceHolder = surfaceHolder;
        mSurfaceHolder.addCallback(this);
    }

    public void setPreviewCallback(Camera.PreviewCallback previewCallback) {
        mPreviewCallback = previewCallback;
    }

    public void startPreview() {
        try {
            Log.e(TAG, "startPreview() mCameraId=" + mCameraId);
            mCamera = Camera.open(mCameraId);
            // 配置摄像头属性
            Camera.Parameters parameters = mCamera.getParameters();
            //设置预览数据格式，nv21
            parameters.setPreviewFormat(ImageFormat.NV21);
            //这是摄像头宽、高
            setPreviewSize(parameters);
            //设置摄像头，图像传感器的角度，方向
            setPreviewOrientation();
            mCamera.setParameters(parameters);

            buffer = new byte[mWidth * mHeight * 3 / 2];
            bytes = new byte[buffer.length];
            //数据缓存区
            mCamera.addCallbackBuffer(buffer);
            mCamera.setPreviewCallbackWithBuffer(this);
            mCamera.setPreviewDisplay(mSurfaceHolder);

            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopPreview() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private void setPreviewOrientation() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, cameraInfo);
        mRotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (mRotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                mOnChangedSizeListener.onChanged(mHeight, mWidth);
                break;
            case Surface.ROTATION_90: // 横屏 左边是头部(home键在右边)
                degrees = 90;
                mOnChangedSizeListener.onChanged(mWidth, mHeight);
                break;
//            case Surface.ROTATION_180:
//                degrees = 180;
//                mOnChangedSizeListener.onChanged(mWidth, mHeight);
//                break;
            case Surface.ROTATION_270: // 横屏 头部在右边
                degrees = 270;
                mOnChangedSizeListener.onChanged(mWidth, mHeight);
                break;
            default:
                break;
        }

        int result;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (cameraInfo.orientation + 360) % 360;
        }

        mCamera.setDisplayOrientation(result);
    }

    private void setPreviewSize(Camera.Parameters parameters) {
        //获取摄像头支持的宽、高
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size size = supportedPreviewSizes.get(0);
        Log.d(TAG, "setPreviewSize() width=" + size.width + " ,height=" + size.height);

        //选择一个与设置的差距最小的支持分辨率

        int m = Math.abs(size.height * size.width - mHeight * mWidth);
        supportedPreviewSizes.remove(0);
        Iterator<Camera.Size> iterator = supportedPreviewSizes.iterator();


        while (iterator.hasNext()) {
            Camera.Size next = iterator.next();
            Log.d(TAG, "支持：" + next.width + " ," + next.height);
            int n = Math.abs(next.height * next.width - mHeight * mWidth);
            if (n < m) {
                m = n;
                size = next;
            }
        }

        mWidth = size.width;
        mHeight = size.height;
        parameters.setPreviewSize(mWidth, mHeight);
        Log.d(TAG, "设置预览分辨率 width:" + size.width + " height:" + size.height);
    }


    /**
     * SurfaceHolder回调
     *
     * @param holder
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    /**
     * SurfaceHolder回调
     *
     * @param holder
     * @param format
     * @param width
     * @param height
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //释放摄像头
        stopPreview();
        //开启摄像头
        startPreview();
    }

    /**
     * SurfaceHolder回调
     *
     * @param holder
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreview();
    }

    /**
     * 相机回调
     *
     * @param data
     * @param camera
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // data数据依然是倒的
        switch (mRotation) {
            case Surface.ROTATION_0:
                rotation90(data);
                break;
            case Surface.ROTATION_90: // 横屏 左边是头部(home键在右边)
                break;
            case Surface.ROTATION_270:// 横屏 头部在右边
                break;
        }


        mPreviewCallback.onPreviewFrame(bytes, camera);
        camera.addCallbackBuffer(buffer);
    }

    private void rotation90(byte[] data) {
        int index = 0;
        int ySize = mWidth * mHeight;
        //u和v
        int uvHeight = mHeight / 2;
        //后置摄像头顺时针旋转90度
        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            //将y的数据旋转之后 放入新的byte数组
            for (int i = 0; i < mWidth; i++) {
                for (int j = mHeight - 1; j >= 0; j--) {
                    bytes[index++] = data[mWidth * j + i];
                }
            }

            //每次处理两个数据
            for (int i = 0; i < mWidth; i += 2) {
                for (int j = uvHeight - 1; j >= 0; j--) {
                    // v
                    bytes[index++] = data[ySize + mWidth * j + i];
                    // u
                    bytes[index++] = data[ySize + mWidth * j + i + 1];
                }
            }
        } else {
            //逆时针旋转90度
            for (int i = 0; i < mWidth; i++) {
                int nPos = mWidth - 1;
                for (int j = 0; j < mHeight; j++) {
                    bytes[index++] = data[nPos - i];
                    nPos += mWidth;
                }
            }
            //u v
            for (int i = 0; i < mWidth; i += 2) {
                int nPos = ySize + mWidth - 1;
                for (int j = 0; j < uvHeight; j++) {
                    bytes[index++] = data[nPos - i - 1];
                    bytes[index++] = data[nPos - i];
                    nPos += mWidth;
                }
            }
        }
    }

    public void setOnChangedSizeListener(OnChangedSizeListener listener) {
        mOnChangedSizeListener = listener;
    }

    public interface OnChangedSizeListener {
        void onChanged(int w, int h);
    }
}
