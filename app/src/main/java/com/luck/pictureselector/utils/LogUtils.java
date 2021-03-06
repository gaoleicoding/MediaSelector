package com.luck.pictureselector.utils;

import android.text.TextUtils;
import android.util.Log;

import com.luck.pictureselector.BuildConfig;

import java.util.List;

/**
 * 日志输出控制类 (Description)
 */
public class LogUtils {
    /** 日志输出级别NONE */
    private static final int LEVEL_NONE = 0;
    /** 日志输出级别V */
    private static final int LEVEL_VERBOSE = 1;
    /** 日志输出级别D */
    private static final int LEVEL_DEBUG = 2;
    /** 日志输出级别I */
    private static final int LEVEL_INFO = 3;
    /** 日志输出级别W */
    private static final int LEVEL_WARN = 4;
    /** 日志输出级别E */
    private static final int LEVEL_ERROR = 5;

    /** 日志输出时的TAG */
    private static String mTag = "QiFu";
    /** 是否允许输出log */
    private static int mDebuggable = BuildConfig.DEBUG ? LEVEL_ERROR : LEVEL_NONE;

    /** 用于记时的变量 */
    private static long mTimestamp = 0;


    /** 以级别为 d 的形式输出LOG */
    public static void v(String tag, String msg) {
        if (mDebuggable >= LEVEL_VERBOSE) {
            Log.v(tag, msg);
        }
    }

    /** 以级别为 d 的形式输出LOG */
    public static void d(String tag, String msg) {
        if (mDebuggable >= LEVEL_DEBUG) {
            Log.d(tag, msg);
        }
    }

    /** 以级别为 i 的形式输出LOG */
    public static void i(String tag, String msg) {
        if (mDebuggable >= LEVEL_INFO) {
            Log.i(tag, msg);
        }
    }

    /** 以级别为 w 的形式输出LOG */
    public static void w(String tag, String msg) {
        if (mDebuggable >= LEVEL_WARN) {
            Log.w(tag, msg);
        }
    }

    /** 以级别为 w 的形式输出LOG信息和Throwable */
    public static void w(String tag, String msg, Throwable tr) {
        if (mDebuggable >= LEVEL_WARN && null != msg) {
            Log.w(tag, msg, tr);
        }
    }

    /** 以级别为 e 的形式输出LOG */
    public static void e(String tag, String msg) {
        if (mDebuggable >= LEVEL_ERROR) {
            Log.e(tag, msg);
        }
    }

    /** 以级别为 e 的形式输出LOG信息和Throwable */
    public static void e(String tag, String msg, Throwable tr) {
        if (mDebuggable >= LEVEL_ERROR && null != msg) {
            Log.e(tag, msg, tr);
        }
    }
    //--------------------------------------------------
    /** 以级别为 d 的形式输出LOG */
    public static void v(String msg) {
        if (mDebuggable >= LEVEL_VERBOSE) {
            Log.v(mTag, msg);
        }
    }

    /** 以级别为 d 的形式输出LOG */
    public static void d(String msg) {
        if (mDebuggable >= LEVEL_DEBUG) {
            Log.d(mTag, msg);
        }
    }

    /** 以级别为 i 的形式输出LOG */
    public static void i(String msg) {
        if (mDebuggable >= LEVEL_INFO) {
            Log.i(mTag, msg);
        }
    }

    /** 以级别为 w 的形式输出LOG */
    public static void w(String msg) {
        if (mDebuggable >= LEVEL_WARN) {
            Log.w(mTag, msg);
        }
    }

    /** 以级别为 w 的形式输出Throwable */
    public static void w(Throwable tr) {
        if (mDebuggable >= LEVEL_WARN) {
            Log.w(mTag, "", tr);
        }
    }

    /** 以级别为 w 的形式输出LOG信息和Throwable */
    public static void w(String msg, Throwable tr) {
        if (mDebuggable >= LEVEL_WARN && null != msg) {
            Log.w(mTag, msg, tr);
        }
    }

    /** 以级别为 e 的形式输出LOG */
    public static void e(String msg) {
        if (mDebuggable >= LEVEL_ERROR) {
            Log.e(mTag, msg);
        }
    }

    /** 以级别为 e 的形式输出Throwable */
    public static void e(Throwable tr) {
        if (mDebuggable >= LEVEL_ERROR) {
            Log.e(mTag, "", tr);
        }
    }

    /** 以级别为 e 的形式输出LOG信息和Throwable */
    public static void e(String msg, Throwable tr) {
        if (mDebuggable >= LEVEL_ERROR && null != msg) {
            Log.e(mTag, msg, tr);
        }
    }




    /**
     * 以级别为 e 的形式输出msg信息,附带时间戳，用于输出一个时间段起始点
     *
     * @param msg
     *            需要输出的msg
     */
    @SuppressWarnings("unused")
    public static void msgStartTime(String msg) {
        mTimestamp = System.currentTimeMillis();
        if (!TextUtils.isEmpty(msg)) {
            e("[Started：" + mTimestamp + "]" + msg);
        }
    }

    /** 以级别为 e 的形式输出msg信息,附带时间戳，用于输出一个时间段结束点* @param msg 需要输出的msg */
    @SuppressWarnings("unused")
    public static void elapsed(String msg) {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - mTimestamp;
        mTimestamp = currentTime;
        e("[Elapsed：" + elapsedTime + "]" + msg);
    }

    public static <T> void printList(List<T> list) {
        if (list == null || list.size() < 1) {
            return;
        }
        int size = list.size();
        i("---begin---");
        for (int i = 0; i < size; i++) {
            i(i + ":" + list.get(i).toString());
        }
        i("---end---");
    }

    @SuppressWarnings("unused")
    public static <T> void printArray(T[] array) {
        if (array == null || array.length < 1) {
            return;
        }
        int length = array.length;
        i("---begin---");
        for (int i = 0; i < length; i++) {
            i(i + ":" + array[i].toString());
        }
        i("---end---");
    }
}