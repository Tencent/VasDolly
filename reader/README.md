# Reader模块
提供了更底层的类：`ChannelReader`和`IdValueReader`，用于满足开发者的精细化需求。

## ChannelReader
主要提供了基于V1和V2签名读取渠道信息的接口，以及判断是否包含V1和V2签名的接口，详情可参见[ChannelReader](https://github.com/Tencent/VasDolly/blob/master/reader/src/main/java/com/leon/channel/reader/ChannelReader.java)

## IdValueReader
主要提供了基于V2签名，读取所有或任意自定义ID-Value的接口，详情可参见[IdValueReader](https://github.com/Tencent/VasDolly/blob/master/reader/src/main/java/com/leon/channel/reader/IdValueReader.java)

## 接入方式
```
dependencies {
    compile 'com.leon.channel:reader:2.0.1'
}
```
