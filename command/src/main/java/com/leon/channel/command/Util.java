package com.leon.channel.command;

import com.leon.channel.common.V1SchemeUtil;
import com.leon.channel.common.V2SchemeUtil;
import com.leon.channel.reader.ChannelReader;

import java.io.File;

/**
 * Created by zys on 17-6-8.
 */

public class Util {

    private static final String V1 = "V1";
    private static final String V2 = "V2";
    private static final String V1_V2 = "V1_V2";

    /**
     * 获取APK的签名方式
     *
     * @param apkFile
     * @return V1, V2, V1_V2
     */
    public static String getSignMode(File apkFile) {

        if (V2SchemeUtil.containV2Signature(apkFile, true)) {
            //如果有V2签名段，并且没有CERT.SF，那么一定是仅仅V2签名,否则就是V1和V2一起签名的
            if (!V1SchemeUtil.containV1Signature(apkFile)) {
                return V2;
            } else {
                return V1_V2;
            }
        } else if (V1SchemeUtil.containV1Signature(apkFile)) {//如果没有V2签名段，并且有CERT.SF，那么一定是仅仅V1签名
            return V1;
        } else {
            return "Apk was not signed";
        }
    }

    /**
     * 获取已知APK的渠道信息
     * @param apkFile
     * @return
     */
    public static String getChannel(File apkFile) {
        String channel = ChannelReader.getChannel(apkFile);
        if (channel == null) {
            channel = ChannelReader.getChannelByZipComment(apkFile);
        }
        return channel;
    }

}
