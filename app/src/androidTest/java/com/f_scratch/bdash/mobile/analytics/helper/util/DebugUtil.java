package com.f_scratch.bdash.mobile.analytics.helper.util;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;

import static android.content.Context.ACTIVITY_SERVICE;

/**
 *
 */
public class DebugUtil {

    /**
     *
     * @param context
     * @return
     */
    public static String getDetailMemoryString( Context context ) {
        int memoryClass = ((ActivityManager) context.getSystemService(ACTIVITY_SERVICE)).getMemoryClass();
        int largeMemoryClass = ((ActivityManager) context.getSystemService(ACTIVITY_SERVICE)).getLargeMemoryClass();

        // メモリ情報を取得
        ActivityManager activityManager = (ActivityManager)context.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        int avaliMem = (int) (memoryInfo.availMem / 1024 / 1024);
        int threshold = (int) (memoryInfo.threshold / 1024 / 1024);
        boolean lowMemory = memoryInfo.lowMemory;


        int nativeAllocate = (int) (Debug.getNativeHeapAllocatedSize() / 1024 / 1024);
        int dalvikTotal = (int) (Runtime.getRuntime().totalMemory() / 1024 / 1024);
        int dalvikFree = (int) (Runtime.getRuntime().freeMemory() / 1024 / 1024);

        int javaAllocate = dalvikTotal - dalvikFree;

        int totalAllocate = nativeAllocate + javaAllocate;

        int ratio = (int)((double) totalAllocate / memoryClass * 100);
        int largeRatio = (int)((double) totalAllocate / largeMemoryClass * 100);


        return "使用可能メモリ = " + String.valueOf(memoryClass) + " MB\n"
                + "使用可能メモリ(large) = " + largeMemoryClass + " MB\n"
                + "native割当済み = " + nativeAllocate + " MB\n"
                + "java割当済み = " + javaAllocate + " MB\n"
                + "total割当済み = " + totalAllocate + " MB\n"
                + "使用率 = " + ratio + "%\n"
                + "使用率(large) = " + largeRatio + "%\n"
                + "(dalvik最大メモリ = " + dalvikTotal + " MB)\n"
                + "(dalvik空きメモリ = " + dalvikFree + " MB)\n"
                + "availMem = " + avaliMem + " MB\n"
                + "threshold = " + threshold + " MB\n"
                + "lowMemory = " + lowMemory;
    }

    /**
     *
     * @param context
     * @return
     */
    public static String getMemoryString( Context context ) {
        int memoryClass = ((ActivityManager) context.getSystemService(ACTIVITY_SERVICE)).getMemoryClass();
        int largeMemoryClass = ((ActivityManager) context.getSystemService(ACTIVITY_SERVICE)).getLargeMemoryClass();

        // メモリ情報を取得
        ActivityManager activityManager = (ActivityManager)context.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        int avaliMem = (int) (memoryInfo.availMem / 1024 / 1024);
        int threshold = (int) (memoryInfo.threshold / 1024 / 1024);
        boolean lowMemory = memoryInfo.lowMemory;


        int nativeAllocate = (int) (Debug.getNativeHeapAllocatedSize() / 1024 / 1024);
        int dalvikTotal = (int) (Runtime.getRuntime().totalMemory() / 1024 / 1024);
        int dalvikFree = (int) (Runtime.getRuntime().freeMemory() / 1024 / 1024);

        int javaAllocate = dalvikTotal - dalvikFree;

        int totalAllocate = nativeAllocate + javaAllocate;

        int ratio = (int)((double) totalAllocate / memoryClass * 100);
        int largeRatio = (int)((double) totalAllocate / largeMemoryClass * 100);

        return "最大: " + String.valueOf(memoryClass) + " MB"
                + "   使用率: " + ratio + "%"
                + "    Native: " + nativeAllocate + " MB"
                + "    Java: " + javaAllocate + " MB"
//                + "  System: " + avaliMem + " MB";
        ;

    }

    /**
     *
     * @param context
     * @return
     */
    public static int getMemoryRatio( Context context ){
        int memoryClass = ((ActivityManager) context.getSystemService(ACTIVITY_SERVICE)).getMemoryClass();

        // メモリ情報を取得
        ActivityManager activityManager = (ActivityManager)context.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        int nativeAllocate = (int) (Debug.getNativeHeapAllocatedSize() / 1024 / 1024);
        int dalvikTotal = (int) (Runtime.getRuntime().totalMemory() / 1024 / 1024);
        int dalvikFree = (int) (Runtime.getRuntime().freeMemory() / 1024 / 1024);

        int javaAllocate = dalvikTotal - dalvikFree;
        int totalAllocate = nativeAllocate + javaAllocate;
        int ratio = (int)((double) totalAllocate / memoryClass * 100);

        return ratio;
    }

}
