package com.leon.channel.command;

import com.leon.channel.common.ApkSectionInfo;
import com.leon.channel.common.V1SchemeUtil;
import com.leon.channel.common.V2SchemeUtil;
import com.leon.channel.common.verify.ApkSignatureSchemeV2Verifier;
import com.leon.channel.reader.ChannelReader;
import com.leon.channel.writer.ChannelWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by zys on 17-6-8.
 */

public class Util {

    private static final String V1 = "V1";
    private static final String V2 = "V2";
    private static final String V1_V2 = "V1_V2";

    public static final int DEFAULT_MODE = -1;
    public static final int V1_MODE = 1;
    public static final int V2_MODE = 2;


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
     *
     * @param apkFile
     * @return
     */
    public static String readChannel(File apkFile) {
        String channel = ChannelReader.getChannel(apkFile);
        if (channel == null) {
            channel = ChannelReader.getChannelByZipComment(apkFile);
        }
        return channel;
    }

    /**
     * 根据不同的方式写入渠道，并生成apk
     *
     * @param baseApk
     * @param channelList
     * @param outputDir
     */
    public static void writeChannel(File baseApk, List<String> channelList, File outputDir) {
        int mode = judgeChannelPackageMode(baseApk);
        if (mode == V1_MODE) {
            generateV1ChannelApk(baseApk, channelList, outputDir);
        } else if (mode == V2_MODE) {
            generateV2ChannelApk(baseApk, channelList, outputDir);
        } else {
            throw new IllegalStateException("not have precise channel package mode");
        }
    }

    /**
     * 首先判断是否有V2签名，如果有就往V2签名段中写入渠道信息，否则就判断V1，如果有V1就在apk注释添加渠道
     * 第三种情况就是没有准确的签名
     *
     * @param baseApk
     * @return
     * @link https://source.android.com/security/apksigning/v2
     */
    private static int judgeChannelPackageMode(File baseApk) {
        if (V2SchemeUtil.containV2Signature(baseApk, false)) {
            return V2_MODE;
        } else if (V1SchemeUtil.containV1Signature(baseApk)) {
            return V1_MODE;
        } else {
            return DEFAULT_MODE;
        }
    }

    /**
     * V1方式写入渠道
     * @param baseApk
     * @param channelList
     * @param outputDir
     */
    private static void generateV1ChannelApk(File baseApk, List<String> channelList, File outputDir) {
        String apkName = baseApk.getName();
        System.out.println("------ File " + apkName + " generate v1 channel apk  , begin ------");

        if (!V1SchemeUtil.containV1Signature(baseApk)) {
            System.out.println("File " + baseApk.getName() + " not signed by v1 , please check your signingConfig , if not have v1 signature , you can't install Apk below 7.0");
        }

        for (String channel : channelList) {
            String apkChannelName = getChannelApkName(apkName, channel);
            System.out.println("generateV1ChannelApk , channel = " + channel + " , apkChannelName = " + apkChannelName);
            File destFile = new File(outputDir, apkChannelName);
            try {
                long before =System.currentTimeMillis();
                System.out.println("before copy");
                copyFile(baseApk, destFile);
                System.out.println("after copy  "+(System.currentTimeMillis()-before) +"millis");

                V1SchemeUtil.writeChannel(destFile, channel);
                //verify channel info
                if (V1SchemeUtil.verifyChannel(destFile, channel)) {
                    System.out.println("generateV1ChannelApk , +" + destFile + " add channel success");
                } else {
                    System.out.println("generateV1ChannelApk ,  +" + destFile + " add channel failure");
                }
                //verify v1 signature
                if (VerifyApk.verifyV1Signature(destFile)) {
                    System.out.println("generateV1ChannelApk , after add channel , apk " + destFile + " v1 verify success");
                } else {
                    System.out.println("generateV1ChannelApk , after add channel , apk " + destFile + " v1 verify failure");
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("generateV2ChannelApk , after add channel , apk " + apkName + " v2 verify failure");
            }

        }
        System.out.println("------ File " + apkName + " generate v1 channel apk , end ------");
    }

    /**
     *  V2方式写入渠道
     * @param baseApk
     * @param channelList
     * @param outputDir
     */
    private static void generateV2ChannelApk(File baseApk, List<String> channelList, File outputDir) {

        String apkName = baseApk.getName();
        System.out.println("------ File " + apkName + " generate v2 channel apk  , begin ------");

        ApkSectionInfo apkSectionInfo = null;
        try {
            apkSectionInfo = V2SchemeUtil.getApkSectionInfo(baseApk);

            for (String channel : channelList) {
                String apkChannelName = getChannelApkName(apkName, channel);
                System.out.println("generateV2ChannelApk , channel = " + channel + " , apkChannelName = " + apkChannelName);
                File destFile= new File(outputDir, apkChannelName);
                copyFile(baseApk, destFile);
                ChannelWriter.addChannel(apkSectionInfo, destFile, channel);

                //verify channel info
                if (ChannelReader.verifyChannel(destFile, channel)) {
                    System.out.println("generateV2ChannelApk , " + destFile + " add channel success");
                } else {
                    System.out.println("generateV2ChannelApk ,  " + destFile + " add channel failure");
                }
                try {
                    //verify v2 signature
                    if (VerifyApk.verifyV2Signature(destFile)) {
                        System.out.println("generateV2ChannelApk , after add channel , apk " + destFile + " v2 verify success");
                    } else {
                        System.out.println("generateV2ChannelApk , after add channel , apk " + destFile + " v2 verify failure");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("generateV2ChannelApk , after add channel , apk " + apkName + " v2 verify failure");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ApkSignatureSchemeV2Verifier.SignatureNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("------ File " + apkName + " generate v2 channel apk , end ------");
    }

    /**
     * 配置添加渠道信息之后的apk名称
     * 由于是命令行的方式
     *
     * @param baseApkName
     * @param channel
     * @return
     */
    private static String getChannelApkName(String baseApkName, String channel) {
        return channel + "-" + baseApkName;
    }


    /**
     * 通过调用cp命令来加快文件复制
     * @param baseApk
     * @param destApk
     */
    private static void copyFile(File baseApk,File destApk) {
        try {
            String command = "cp "+baseApk.getAbsolutePath()+" "+destApk.getAbsolutePath();
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取渠道文件
     * @param channelFile
     * @return
     */
    public static List<String> readChannelFile(File channelFile){
        ArrayList<String> channelList = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(channelFile));
            String s;
            try {
                while ((s = br.readLine()) != null) {
                    channelList.add(s);
                }
                return channelList;
            } finally {
                br.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
