package com.wwb.live;

public class TestNdkHandler {

    public static native int getVersion();


    static {
        System.loadLibrary("ffmpeg");
    }
}
