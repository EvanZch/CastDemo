package com.example.castdemo;

import android.util.Log;

public class LogUtil {
    private static Boolean DEBUG = true;
    public static String TAG = "CastDemo";

    private static void logcat(String tag, String msg) {
        Log.d(tag, msg);
    }

    public static void d(String tag, String msg) {
        if (DEBUG) {
            logcat(tag, msg);
        }
    }

    public static void i(String msg) {
        Log.i(TAG, msg);
    }


    public static void d(String msg) {
        if (DEBUG) {
            logcat(TAG, msg);
        }
    }

    public static void e(String tag, String msg, Exception e) {
        Log.e(tag, msg, e);
    }

    public static void e(String msg, Exception e) {
        Log.e(TAG, msg, e);
    }

    public static void e(String msg, String e) {
        if (DEBUG) {
            Log.e(TAG, e);
        }
    }

    public static void e(String msg) {

        if (DEBUG) {
            Log.e(TAG, msg);
        }
    }


}
