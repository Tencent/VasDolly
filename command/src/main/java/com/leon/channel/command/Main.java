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
        String cmdHelp = "help";

        String help = "The commands are:\n\n" +
                "global options:\n\n" +
                "    " + cmdGet + "                get apk information\n" +
                "    " + cmdPut + "                put channel information\n" +
                "    " + cmdHelp + "               get help\n\n" +
                "general args:\n\n" +
                "    " + cmdSignMode + "[arg]                 signature mode\n" +
                "    " + cmdChannel + "[arg]                 channel information\n\n" +
                "for example:\n\n" +
                "    java -jar ApkChannelPackage.jar get -c /home/user/test.apk\n" +
                "    java -jar ApkChannelPackage.jar put -c \"channel1,channel2\" /home/user/base.apk /home/user/output.apk\n" +
                "    java -jar ApkChannelPackage.jar put -c channel.txt /home/user/base.apk /home/user/output.apk\n\n"+
                "Use commas to write multiple channels,you can also use channel file.\n";

        if (args.length == 0 || args[0] == null || args[0].trim().length() == 0) {
            System.out.print(help);
        } else if (args.length > 0) {
            if (args.length == 1) {
                if (args[0].trim().equals(cmdHelp)) {
                    System.out.print(help);
                } else {
                    System.out.print("\nPlease enter the correct command!");
                }
            } else if (args.length == 2) {
                System.out.print("\nPlease enter the correct command!");
            } else {
                String command0 = args[0].trim();
                String command1 = args[1].trim();
                if (command0.equals(cmdGet)) {//获取
                    String filePath = args[args.length - 1].trim();
                    File file = new File(filePath);
                    if (!file.exists()) {
                        System.out.print("\nApk file does not exist!");
                        return;
                    } else if (file.isDirectory()) {
                        System.out.print("\nThe file path cannot be a directory!");
                        return;
                    }
                    if (command1.equals(cmdSignMode)) {//获取签名方式
                        System.out.print("\nsignature mode:" + Util.getSignMode(file));
                    } else if (command1.equals(cmdChannel)) {//获取渠道信息
                        System.out.print("\nChannel: " + Util.readChannel(file));
                    } else {
                        System.out.print("\nPlease enter the correct command!");
                    }
                } else if (command0.equals(cmdPut)) {//插入
                    if (command1.equals(cmdChannel)) {//插入渠道信息
                        //baseApk
                        String baseApkPath = args[args.length - 2].trim();
                        File baseApk = new File(baseApkPath);
                        if (!baseApk.exists()) {
                            System.out.print("\nApk file does not exist!");
                            return;
                        } else if (baseApk.isDirectory()) {
                            System.out.print("\nThe file path cannot be a directory!");
                            return;
                        }
                        //base
                        String outPutPath = args[args.length - 1].trim();
                        File outPutDir = new File(outPutPath);
                        if (!outPutDir.isDirectory()) {
                            System.out.print("\nThe output path cannot be a file!");
                            return;
                        }
                        //渠道信息
                        String channels = args[2].trim();
                        File channelFile = new File(channels);
                        List<String> channelList = new ArrayList<>();
                        //渠道文件
                        if(channelFile.exists()&&!channelFile.isDirectory()){
                            channelList= Util.readChannelFile(channelFile);
                        }else {
                            String[] channelArray = channels.split(",");
                            channelList= Arrays.asList(channelArray);
                        }
                        Util.writeChannel(baseApk, channelList, outPutDir);
                    }
                } else {
                    System.out.print("\nPlease enter the correct command!");
                }
            }
        }
    }


}

