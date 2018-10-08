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

package com.leon.channel.command;

import com.leon.channel.verify.VerifyApk;
import com.leon.channel.common.ApkSectionInfo;
import com.leon.channel.common.verify.ApkSignatureSchemeV2Verifier;
import com.leon.channel.reader.ChannelReader;
import com.leon.channel.writer.ChannelWriter;
import com.leon.channel.writer.IdValueWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
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

        if (ChannelReader.containV2Signature(apkFile)) {
            //如果有V2签名段，并且没有CERT.SF，那么一定是仅仅V2签名,否则就是V1和V2一起签名的
            if (!ChannelReader.containV1Signature(apkFile)) {
                return V2;
            } else {
                return V1_V2;
            }
        } else if (ChannelReader.containV1Signature(apkFile)) { //如果没有V2签名段，并且有CERT.SF，那么一定是仅仅V1签名
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
        String channel = ChannelReader.getChannelByV2(apkFile);
        if (channel == null) {
            channel = ChannelReader.getChannelByV1(apkFile);
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
    public static void writeChannel(File baseApk, List<String> channelList, File outputDir, boolean isMultiThread, boolean isFastMode) {
        if (channelList.isEmpty()) {
            System.out.println("channel list is empty , please set channel list");
            return;
        }
        int mode = judgeChannelPackageMode(baseApk);
        if (mode == V1_MODE) {
            System.out.println("baseApk : " + baseApk.getAbsolutePath() + " , ChannelPackageMode : V1 Mode , isMultiThread : " + isMultiThread + " , isFastMode : " + isFastMode);
            if (isMultiThread) {
                generateV1ChannelApkMultiThread(baseApk, channelList, outputDir, isFastMode);
            } else {
                generateV1ChannelApk(baseApk, channelList, outputDir, isFastMode);
            }
        } else if (mode == V2_MODE) {
            System.out.println("baseApk : " + baseApk.getAbsolutePath() + " , ChannelPackageMode : V2 Mode" + " , isFastMode : " + isFastMode);
            generateV2ChannelApk(baseApk, channelList, outputDir, isFastMode);
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
        if (ChannelReader.containV2Signature(baseApk)) {
            return V2_MODE;
        } else if (ChannelReader.containV1Signature(baseApk)) {
            return V1_MODE;
        } else {
            return DEFAULT_MODE;
        }
    }

    /**
     * V1方式写入渠道
     *
     * @param baseApk
     * @param channelList
     * @param outputDir
     */
    private static void generateV1ChannelApk(File baseApk, List<String> channelList, File outputDir, boolean isFastMode) {
        if (!ChannelReader.containV1Signature(baseApk)) {
            System.out.println("File " + baseApk.getName() + " not signed by v1 , please check your signingConfig , if not have v1 signature , you can't install Apk below 7.0");
            return;
        }

        String apkName = baseApk.getName();
        //判断基础包是否已经包含渠道信息

        String testChannel = ChannelReader.getChannelByV1(baseApk);
        if (testChannel != null) {
            System.out.println("baseApk : " + baseApk.getAbsolutePath() + " has a channel : " + testChannel + ", only ignore");
            return;
        }

        long startTime = System.currentTimeMillis();
        System.out.println("------ File " + apkName + " generate v1 channel apk  , begin ------");

        try {
            for (String channel : channelList) {
                String apkChannelName = getChannelApkName(apkName, channel);
                System.out.println("generateV1ChannelApk , channel = " + channel + " , apkChannelName = " + apkChannelName);
                File destFile = new File(outputDir, apkChannelName);
                copyFileUsingNio(baseApk, destFile);
                ChannelWriter.addChannelByV1(destFile, channel);
                if (!isFastMode) {
                    //1. verify channel info
                    if (ChannelReader.verifyChannelByV1(destFile, channel)) {
                        System.out.println("generateV1ChannelApk , " + destFile + " add channel success");
                    } else {
                        throw new RuntimeException("generateV1ChannelApk , " + destFile + " add channel failure");
                    }
                    //2. verify v1 signature
                    if (VerifyApk.verifyV1Signature(destFile)) {
                        System.out.println("generateV1ChannelApk , after add channel , " + destFile + " v1 verify success");
                    } else {
                        throw new RuntimeException("generateV1ChannelApk , after add channel , " + destFile + " v1 verify failure");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("generateV1ChannelApk error , please check it and fix it ，and that you should generate all V1 Channel Apk again!");
            e.printStackTrace();
        }

        System.out.println("------ File " + apkName + " generate v1 channel apk , end ------");
        long cost = System.currentTimeMillis() - startTime;
        System.out.println("------ total " + channelList.size() + " channel apk , cost : " + cost + " ------");
    }

    /**
     * V1方式写入渠道 (多线程)
     *
     * @param baseApk
     * @param channelList
     * @param outputDir
     */
    private static void generateV1ChannelApkMultiThread(File baseApk, List<String> channelList, File outputDir, boolean isFastMode) {
        if (!ChannelReader.containV1Signature(baseApk)) {
            System.out.println("File " + baseApk.getName() + " not signed by v1 , please check your signingConfig , if not have v1 signature , you can't install Apk below 7.0");
            return;
        }

        String apkName = baseApk.getName();
        //判断基础包是否已经包含渠道信息
        String testChannel = ChannelReader.getChannelByV1(baseApk);
        if (testChannel != null) {
            System.out.println("baseApk : " + baseApk.getAbsolutePath() + " has a channel : " + testChannel + ", only ignore");
            return;
        }

        long startTime = System.currentTimeMillis();
        System.out.println("------ File " + apkName + " generate v1 channel apk  , begin ------");
        //多线程生成渠道包
        ThreadManager.getInstance().generateV1Channel(baseApk, channelList, outputDir, isFastMode);
        ThreadManager.getInstance().destory();
        System.out.println("------ File " + apkName + " generate v1 channel apk , end ------");
        long cost = System.currentTimeMillis() - startTime;
        System.out.println("------ total " + channelList.size() + " channel apk , cost : " + cost + " ------");
    }

    /**
     * V2方式写入渠道
     *
     * @param baseApk
     * @param channelList
     * @param outputDir
     */
    private static void generateV2ChannelApk(File baseApk, List<String> channelList, File outputDir, boolean isFastMode) {
        String apkName = baseApk.getName();
        long startTime = System.currentTimeMillis();
        System.out.println("------ File " + apkName + " generate v2 channel apk  , begin ------");

        try {
            ApkSectionInfo apkSectionInfo = IdValueWriter.getApkSectionInfo(baseApk, false);
            for (String channel : channelList) {
                String apkChannelName = getChannelApkName(apkName, channel);
                System.out.println("generateV2ChannelApk , channel = " + channel + " , apkChannelName = " + apkChannelName);
                File destFile = new File(outputDir, apkChannelName);
                if (apkSectionInfo.lowMemory) {
                    copyFileUsingNio(baseApk, destFile);
                }
                ChannelWriter.addChannelByV2(apkSectionInfo, destFile, channel);
                if (!isFastMode) {
                    //1. verify channel info
                    if (ChannelReader.verifyChannelByV2(destFile, channel)) {
                        System.out.println("generateV2ChannelApk , " + destFile + " add channel success");
                    } else {
                        throw new RuntimeException("generateV2ChannelApk , " + destFile + " add channel failure");
                    }

                    //2. verify v2 signature
                    if (VerifyApk.verifyV2Signature(destFile)) {
                        System.out.println("generateV2ChannelApk , after add channel ,  " + destFile + " v2 verify success");
                    } else {
                        throw new RuntimeException("generateV2ChannelApk , after add channel , " + destFile + " v1 verify failure");
                    }
                }
                apkSectionInfo.rewind();
                if (!isFastMode) {
                    apkSectionInfo.checkEocdCentralDirOffset();
                }
            }
        } catch (Exception e) {
            System.out.println("generateV2ChannelApk error , please check it and fix it ，and that you should generate all V2 Channel Apk again!");
            e.printStackTrace();
        }

        System.out.println("------ File " + apkName + " generate v2 channel apk , end ------");
        long cost = System.currentTimeMillis() - startTime;
        System.out.println("------ total " + channelList.size() + " channel apk , cost : " + cost + " ------");
    }


    public static boolean removeChannel(File channelApk) {
        int mode = judgeChannelPackageMode(channelApk);
        if (mode == V1_MODE) {
            System.out.println("Now we dissupport to remove channel info from apk in the v1 signature mode , we will achieve it soon! ");
        } else if (mode == V2_MODE) {
            try {
                ChannelWriter.removeChannelByV2(channelApk, true);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ApkSignatureSchemeV2Verifier.SignatureNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            throw new IllegalStateException("not have precise channel package mode");
        }
        return false;
    }

    /**
     * 配置添加渠道信息之后的apk名称
     *
     * @param baseApkName
     * @param channel
     * @return
     */
    public static String getChannelApkName(String baseApkName, String channel) {
        if (baseApkName.contains("base")) {
            return baseApkName.replace("base", channel);
        }
        return channel + "-" + baseApkName;
    }


    /**
     * 通过调用cp命令来加快文件复制
     *
     * @param baseApk
     * @param destApk
     */
    private static void copyFileByCp(File baseApk, File destApk) {
        try {
            String command = "cp " + baseApk.getAbsolutePath() + " " + destApk.getAbsolutePath();
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 使用文件流copy文件
     *
     * @param source
     * @param dest
     * @throws IOException
     */
    public static void copyFileUsingStream(File source, File dest) throws IOException {
        FileInputStream is = null;
        FileOutputStream os = null;
        File parent = dest.getParentFile();
        if (parent != null && (!parent.exists())) {
            parent.mkdirs();
        }
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest, false);

            byte[] buffer = new byte[4096];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }
    }

    public static void copyFileUsingNio(File source, File dest) throws IOException {
        FileChannel in = null;
        FileChannel out = null;
        FileInputStream inStream = null;
        FileOutputStream outStream = null;
        File parent = dest.getParentFile();
        if (parent != null && (!parent.exists())) {
            parent.mkdirs();
        }
        try {
            inStream = new FileInputStream(source);
            outStream = new FileOutputStream(dest, false);
            in = inStream.getChannel();
            out = outStream.getChannel();
            in.transferTo(0, in.size(), out);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inStream != null) {
                inStream.close();
            }
            if (outStream != null) {
                outStream.close();
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * 读取渠道文件
     *
     * @param channelFile
     * @return
     */
    public static List<String> readChannelFile(File channelFile) {
        ArrayList<String> channelList = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(channelFile));
            String line;
            try {
                while ((line = br.readLine()) != null) {
                    String[] array = line.split("#");
                    if (array != null && array.length > 0 && array[0] != null && array[0].trim().length() > 0) {
                        channelList.add(array[0].trim());
                    } else {
                        System.out.println("skip invalid channel line : " + line);
                    }
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
