/*
 * Tencent is pleased to support the open source community by making VasDolly available.
 *
 * Copyright (C) 2018 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS,WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    private static String mChannelCache;


    public static String getChannel(Context context) {
        if (mChannelCache == null) {
            String channel = getChannelByV2(context);
            if (channel == null) {
                channel = getChannelByV1(context);
            }
            mChannelCache = channel;
        }

        return mChannelCache;
    }

    /**
     * if apk use v2 signature , please use this method to get channel info
     *
     * @param context
     * @return
     */
    public static String getChannelByV2(Context context) {
        String apkPath = getApkPath(context);
        String channel = ChannelReader.getChannelByV2(new File(apkPath));
        Log.i(TAG, "getChannelByV2 , channel = " + channel);
        return channel;
    }

    /**
     * if apk only use v1 signature , please use this method to get channel info
     *
     * @param context
     * @return
     */
    public static String getChannelByV1(Context context) {
        String apkPath = getApkPath(context);
        String channel = ChannelReader.getChannelByV1(new File(apkPath));
        Log.i(TAG, "getChannelByV1 , channel = " + channel);
        return channel;
    }


    /**
     * get String value from apk by id in the v2 signature mode
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
     * get byte[] from apk by id in the v2 signature mode
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
     * find all Id-Value Pair from Apk in the v2 signature mode
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
