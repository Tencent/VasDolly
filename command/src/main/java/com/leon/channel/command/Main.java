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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Created by zys on 17-6-8.
 */

public class Main {

    public static void main(String[] args) {
        String cmdGet = "get";//获取
        String cmdPut = "put";//插入
        String cmdSignMode = "-s";//签名方式
        String cmdChannel = "-c";//渠道信息
        String cmdMultiThreadChannel = "-mtc";//基于V1签名生成多渠道包时，使用多线程模式
        String cmdHelp = "help";

        String help = "The commands are:\n" +
                "java -jar VasDolly.jar [global options] [general args] [input file] [output directory] \n\n" +
                "global options:\n\n" +
                "    " + cmdGet + "                get apk information\n" +
                "    " + cmdPut + "                put channel information\n" +
                "    " + cmdHelp + "               get help\n\n" +
                "general args:\n\n" +
                "    " + cmdSignMode + " [arg]                 signature mode , only fit 'get'\n" +
                "    " + cmdChannel + " [arg]                 channel information\n" +
                "    " + cmdMultiThreadChannel + " [arg]               multithread to v1 , only fit 'put'\n\n" +
                "for example:\n\n" +
                "    java -jar VasDolly.jar get -s /home/user/test.apk\n" +
                "    java -jar VasDolly.jar get -c /home/user/test.apk\n" +
                "    java -jar VasDolly.jar put -c \"channel1,channel2\" /home/user/base.apk /home/user/\n" +
                "    java -jar VasDolly.jar put -mtc \"channel1,channel2\" /home/user/base.apk /home/user/\n" +
                "    java -jar VasDolly.jar put -c channel.txt /home/user/base.apk /home/user/\n" +
                "    java -jar VasDolly.jar put -mtc channel.txt /home/user/base.apk /home/user/\n\n" +
                "Use commas to write multiple channels , you can also use channel file.\n";

        if (args.length == 0 || args[0] == null || args[0].trim().length() == 0) {
            System.out.print(help);
        } else if (args.length > 0) {
            if (args.length == 1) {
                if (args[0].trim().equals(cmdHelp)) {
                    System.out.print(help);
                } else {
                    System.out.print("\n\nPlease enter the correct command!");
                }
            } else if (args.length == 2) {
                System.out.print("\n\nPlease enter the correct command!");
            } else {
                String command0 = args[0].trim();
                String command1 = args[1].trim();
                if (command0.equals(cmdGet)) { //获取
                    if (args.length >= 3) {
                        String filePath = args[2].trim();
                        File file = new File(filePath);
                        if (!file.exists()) {
                            System.out.print("\n\nApk file does not exist!");
                            return;
                        } else if (file.isDirectory()) {
                            System.out.print("\n\nThe file path cannot be a directory!");
                            return;
                        }
                        if (command1.equals(cmdSignMode)) { //获取签名方式
                            System.out.print("\n\nsignature mode:" + Util.getSignMode(file));
                        } else if (command1.equals(cmdChannel)) {//获取渠道信息
                            System.out.print("\n\nChannel: " + Util.readChannel(file));
                        } else {
                            System.out.print("\n\nPlease enter the correct command!");
                        }
                    } else {
                        System.out.print("\n\nPlease enter the correct command!");
                    }

                } else if (command0.equals(cmdPut)) { //插入
                    if (command1.equals(cmdChannel) || command1.equals(cmdMultiThreadChannel)) { //插入渠道信息
                        boolean isMultiThread = command1.equals(cmdMultiThreadChannel);
                        if (args.length >= 5) {
                            //baseApk
                            String baseApkPath = args[3].trim();
                            File baseApk = new File(baseApkPath);
                            if (!baseApk.exists()) {
                                System.out.print("\n\nApk file does not exist!");
                                return;
                            } else if (baseApk.isDirectory()) {
                                System.out.print("\n\nThe file path cannot be a directory!");
                                return;
                            }
                            //base
                            String outPutPath = args[4].trim();
                            File outPutDir = new File(outPutPath);
                            if (!outPutDir.isDirectory()) {
                                System.out.print("\n\nThe output path cannot be a file , must be a directory!");
                                return;
                            }
                            //渠道信息
                            String channels = args[2].trim();
                            File channelFile = new File(channels);
                            List<String> channelList = new ArrayList<>();
                            //渠道文件
                            if (channelFile.exists() && !channelFile.isDirectory()) {
                                channelList = Util.readChannelFile(channelFile);
                            } else {
                                String[] channelArray = channels.split(",");
                                channelList = Arrays.asList(channelArray);
                            }
                            Util.writeChannel(baseApk, channelList, outPutDir,isMultiThread);
                        } else {
                            System.out.print("\n\nPlease enter the correct command!");
                        }
                    } else {
                        System.out.print("\n\n'put' only support -c arg!");
                    }
                } else {
                    System.out.print("\n\nPlease enter the correct command!");
                }
            }
        }

        System.out.println();
    }

}

