package com.leon.channel.command;

import java.io.File;


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
                "    " + cmdGet + " [ arg ]        get apk information\n" +
                "    " + cmdPut + " [ arg ]        put channel information\n" +
                "    " + cmdHelp + "               get help\n\n" +
                "general args:\n\n" +
                "    " + cmdSignMode + "                 signature mode\n" +
                "    " + cmdChannel + "                 channel information\n\n" +
                "for example:\n\n" +
                "    java -jar ApkChannelPackage.jar get -c /home/user/test.apk\n" +
                "    java -jar ApkChannelPackage.jar put -c \"channel\" /home/user/test.apk\n\n" +
                "Use commas to write multiple channels";

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
                        //
                        String filePath = args[args.length - 2].trim();
                        File file = new File(filePath);
                        if (!file.exists()) {
                            System.out.print("\nApk file does not exist!");
                            return;
                        } else if (file.isDirectory()) {
                            System.out.print("\nThe file path cannot be a directory!");
                            return;
                        }

                        String outPutPath = args[args.length - 1].trim();
                        File outPutDir = new File(outPutPath);
                        if (!outPutDir.isDirectory()) {
                            System.out.print("\nThe output path cannot be a file!");
                            return;
                        }
                        String channels = args[2].trim();
                        String[] channelArray = channels.split(",");
                        Util.writeChannel(file, channelArray, outPutDir);
                    }
                } else {
                    System.out.print("\nPlease enter the correct command!");
                }
            }
        }
    }


}

