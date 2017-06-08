package com.leon.channel.command;

import java.io.File;


/**
 * Created by zys on 17-6-8.
 */

public class Main {


    public static void main(String[] args) {
        String cmdSignMode = "sign-mode";
        String cmdChannel = "channel";
        String cmdFilePath = "-path";
        String cmdHelp = "help";

        String help = "The commands are:\n\n" +
                "global options:\n" +
                "    "+cmdFilePath + " [path]        the file path of apk\n\n" +
                "general commands:\n" +
                "    "+cmdSignMode + "           get signature mode\n" +
                "    "+cmdChannel + "             get channel information\n\n"+
                "for example:\n"+
                "java -jar ApkChannelPackage.jar channel -path /home/user/test.apk";

        if (args.length == 0 || args[0] == null || args[0].trim().length() == 0) {
            System.out.print(help);
        } else if (args.length > 0) {
            String args0 = args[0].trim();
            if (args.length == 1) {
                if (args0.equals(cmdHelp)) {
                    System.out.print(help);
                } else {
                    System.out.print("\nPlease enter the correct command!");
                }
            } else if (args.length == 2) {
                System.out.print("\nPlease enter the correct command!");
            } else if (args.length == 3) {
                String command = args[0].trim();
                String filePath = args[2].trim();
                File file = new File(filePath);
                if (!file.exists()) {
                    System.out.print("\nApk file does not exist!");
                    return;
                } else if (file.isDirectory()) {
                    System.out.print("\nThe file path cannot be a directory!");
                    return;
                }
                if (command.endsWith(cmdSignMode)) {//获取签名方式
                    System.out.print("\nsignature mode:"+Util.getSignMode(file));
                }else if (command.equals(cmdChannel)){//获取渠道信息
                    System.out.print("\nChannel: "+Util.getChannel(file));
                }else {
                    System.out.print("\nPlease enter the correct command!");
                }

            }
        }
    }


}

