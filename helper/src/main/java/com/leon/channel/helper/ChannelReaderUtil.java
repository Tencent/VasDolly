package com.leon.channel.helper;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import com.leon.channel.reader.ChannelReader;
import com.leon.channel.reader.IdValueReader;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Created by leontli on 17/2/12.
 */

public class ChannelReaderUtil {
    private static final String TAG = "ChannelReaderUtil";

    /**
     * get channel info
     *
     * @param context
     * @return
     */
    public static String getChannel(Context context) {
        String apkPath = getApkPath(context);
        String channel = ChannelReader.getChannel(new File(apkPath));
        Log.i(TAG, "channel = " + channel);
        return channel;
    }

    /**
     * get String value from apk by id
     *
     * @param context
     * @param id
     * @return
     */
    public static String getStringValueById(Context context, int id) {
        String apkPath = getApkPath(context);
        String value = IdValueReader.getStringValueById(new File(apkPath), id);
        Log.i(TAG, "id = " + id + " , value = " + value);
        return value;
    }

    /**
     * get byte[] from apk by id
     *
     * @param context
     * @param id
     * @return
     */
    public static byte[] getByteValueById(Context context, int id) {
        String apkPath = getApkPath(context);
        return IdValueReader.getByteValueById(new File(apkPath), id);
    }

    /**
     * find all Id-Value Pair from Apk
     *
     * @param context
     * @return
     */
    public static Map<Integer, ByteBuffer> getAllIdValueMap(Context context) {
        String apkPath = getApkPath(context);
        return IdValueReader.getAllIdValueMap(new File(apkPath));
    }


    /**
     * 获取已安装的APK路径
     *
     * @param context
     * @return
     */
    private static String getApkPath(Context context) {
        String apkPath = null;
        try {
            ApplicationInfo applicationInfo = context.getApplicationInfo();
            if (applicationInfo == null) {
                return null;
            } else {
                apkPath = applicationInfo.sourceDir;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return apkPath;
    }
}
