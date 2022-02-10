package com.huawei.mlkit.sample;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.bytedance.android.bytehook.ByteHook;

public class FyApplication extends Application {
    private String TAG = "byteHook_fy_java_tag";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        // init bytehook
        int r = ByteHook.init(new ByteHook.ConfigBuilder()
                .setMode(ByteHook.Mode.AUTOMATIC)       // 0 自动模式
//                .setMode(ByteHook.Mode.MANUAL)        // 1 手动模式
                .setDebug(true)
                .build());
        Log.i(TAG, "byteHook init, return: " + r);

        // load hacker
        System.loadLibrary("detour");

    }
}
