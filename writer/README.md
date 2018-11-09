# Write模块
提供了更底层的类：`ChannelWriter`和`IdValueWriter`，用于满足开发者的精细化需求。

## ChannelWriter
主要提供了基于V1和V2签名，添加渠道信息的接口，以及对应删除渠道信息的接口，详情可参见[ChannelWriter](https://github.com/Tencent/VasDolly/blob/master/writer/src/main/java/com/leon/channel/writer/ChannelWriter.java)

## IdValueWriter
主要提供了基于V2签名，添加自定义ID-Value的接口，以及对应删除任意ID-Value的接口，详情可参见[IdValueWriter](https://github.com/Tencent/VasDolly/blob/master/writer/src/main/java/com/leon/channel/writer/IdValueWriter.java)

## 接入方式
```
dependencies {
    compile 'com.leon.channel:writer:2.0.1'
}
```
