## VasDolly命令行
 命令行工具即：jar文件下`VasDolly.jar`，可以通过help查看具体命令：
 ``` java
 java -jar VasDolly.jar help
 ```

 目前支持以下操作：

 ### 获取指定APK的签名方式
 ``` java
 java -jar VasDolly.jar get -s /home/user/test.apk
 ```
 ### 获取指定APK的渠道信息
 ``` java
 java -jar VasDolly.jar get -c /home/user/test.apk
 ```
 ### 删除指定APK的渠道信息
 ``` java
 java -jar VasDolly.jar remove -c /home/user/test.apk
 ```
 ### 通过指定渠道字符串添加渠道信息
 ``` java
 java -jar VasDolly.jar put -c "channel1,channel2" /home/user/base.apk /home/user/
 ```
 ### 通过指定某个渠道字符串添加渠道信息到目标APK
 ``` java
 java -jar VasDolly.jar put -c "channel1" /home/user/base.apk /home/user/base.apk
 ```
 ### 通过指定渠道文件添加渠道信息
 ``` java
 java -jar VasDolly.jar put -c channel.txt /home/user/base.apk /home/user/
 ```
 ### 为基于V1的多渠道打包添加了多线程支持，满足渠道较多的使用场景
 ``` java
 java -jar VasDolly.jar put -mtc channel.txt /home/user/base.apk /home/user/
 ```
 ### 提供了FastMode，生成渠道包时不进行强校验，速度可提升10倍以上
 ``` java
 java -jar VasDolly.jar put -c channel.txt -f /home/user/base.apk /home/user/
 ```
