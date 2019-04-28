[![license](http://img.shields.io/badge/license-BSD3-brightgreen.svg?style=flat)](https://github.com/Tencent/VasDolly/blob/master/LICENSE)
[![Release Version](https://img.shields.io/badge/release-2.0.3-red.svg)](https://github.com/Tencent/VasDolly/releases)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/Tencent/VasDolly/pulls)
[![wiki](https://img.shields.io/badge/Wiki-open-brightgreen.svg)](https://github.com/Tencent/VasDolly/wiki)
---


# 简介
VasDolly是一种快速多渠道打包工具，同时支持基于V1签名和V2签名进行多渠道打包。插件本身会自动检测Apk使用的签名类别，并选择合适的多渠道打包方式，对使用者来说完全透明。 
`V1.1.6`版本已支持Android Gradle Plugin 3.0，欢迎使用！

目前Gradle Plugin 2.2以上默认开启V2签名，所以如果想关闭V2签名，可将下面的v2SigningEnabled设置为false。
``` groovy
signingConfigs {
        release {
            ...
            v1SigningEnabled true
            v2SigningEnabled false
        }

        debug {
            ...
            v1SigningEnabled true
            v2SigningEnabled false
        }
    }
```

# 接入流程

## 添加对VasDolly Plugin的依赖
在根工程的`build.gradle`中，添加对打包Plugin的依赖：
``` groovy
dependencies {
        classpath 'com.android.tools.build:gradle:3.0.0'
        classpath 'com.leon.channel:plugin:2.0.3'
}
```
## 引用VasDolly Plugin
在主App工程的`build.gradle`中，添加对VasDolly Plugin的引用：
``` groovy
apply plugin: 'channel'
```
## 添加对VasDolly helper类库的依赖
在主App工程的`build.gradle`中，添加读取渠道信息的helper类库依赖：
``` groovy
dependencies {
    api 'com.leon.channel:helper:2.0.3'
}
```
## 配置渠道列表
目前有两种方式配置渠道列表，最终的渠道列表是两者的累加之和：
1. 在`gradle.properties`文件指定渠道文件名称，该渠道文件必须位于根工程目录下，一行一个渠道信息。
``` groovy
channel_file=channel.txt
```
2. 在`channel`或者`rebuildChannel`属性中通过`channelFile`属性指定渠道文件，一行一个渠道信息。
``` groovy
channel{
    //指定渠道文件
    channelFile = file("/Users/leon/Downloads/testChannel.txt")
}
rebuildChannel{
    //指定渠道文件
    channelFile = file("/Users/leon/Downloads/testReChannel.txt")
}
```
## 通过Gradle生成多渠道包
### 直接编译生成多渠道包
若是直接编译生成多渠道包，首先要配置渠道文件、渠道包的输出目录和渠道包的命名规则：
``` groovy
channel{
    //指定渠道文件
    channelFile = file("/Users/leon/Downloads/testChannel.txt")
     //多渠道包的输出目录，默认为new File(project.buildDir,"channel")
    baseOutputDir = new File(project.buildDir,"xxx")
    //多渠道包的命名规则，默认为：${appName}-${versionName}-${versionCode}-${flavorName}-${buildType}
    apkNameFormat ='${appName}-${versionName}-${versionCode}-${flavorName}-${buildType}'
    //快速模式：生成渠道包时不进行校验（速度可以提升10倍以上，默认为false）
    isFastMode = false
    //buildTime的时间格式，默认格式：yyyyMMdd-HHmmss
    buildTimeDateFormat = 'yyyyMMdd-HH:mm:ss'
    //低内存模式（仅针对V2签名，默认为false）：只把签名块、中央目录和EOCD读取到内存，不把最大头的内容块读取到内存，在手机上合成APK时，可以使用该模式
    lowMemory = false
}
```
其中，多渠道包的命名规则中，可使用以下字段：

* appName ： 当前project的name
* versionName ： 当前Variant的versionName
* versionCode ： 当前Variant的versionCode
* buildType ： 当前Variant的buildType，即debug or release
* flavorName ： 当前的渠道名称
* appId ： 当前Variant的applicationId
* buildTime ： 当前编译构建日期时间，时间格式可以自定义，默认格式：yyyyMMdd-HHmmss

然后，通过`gradle channelDebug`、`gradle channelRelease`命令分别生成Debug和Release的多渠道包。

为了方便临时生成渠道包进行测试，我们从`v2.0.0`开始支持添加渠道参数：`gradle channelDebug(channelRelease) -Pchannels=yingyongbao,gamecenter`，这里通过属性`channels`指定的渠道列表拥有更高的优先级，且和原始的文件方式是互斥的。

### 根据已有基础包重新生成多渠道包
若是根据已有基础包重新生成多渠道包，首先要配置渠道文件、基础包的路径和渠道包的输出目录：
``` groovy
rebuildChannel {
  //指定渠道文件
  channelFile = file("/Users/leon/Downloads/testReChannel.txt")
  baseDebugApk = 已有Debug APK    
  baseReleaseApk = 已有Release APK
  //默认为new File(project.buildDir, "rebuildChannel/debug")
  debugOutputDir = Debug渠道包输出目录   
  //默认为new File(project.buildDir, "rebuildChannel/release")
  releaseOutputDir = Release渠道包输出目录
  //快速模式：生成渠道包时不进行校验（速度可以提升10倍以上，默认为false）
  isFastMode = false
  //低内存模式（仅针对V2签名，默认为false）：只把签名块、中央目录和EOCD读取到内存，不把最大头的内容块读取到内存，在手机上合成APK时，可以使用该模式
  lowMemory = false
}
```
然后，通过`gradle rebuildChannel`命令生成多渠道包。

为了方便临时生成渠道包进行测试，我们从`v2.0.0`开始支持添加渠道参数：`gradle rebuildChannel -Pchannels=yingyongbao,gamecenter`，这里通过属性`channels`指定的渠道列表拥有更高的优先级，且和原始的文件方式是互斥的。

## 通过命令行生成渠道包、读取渠道信息
从`V1.0.5`版本开始支持命令行，具体使用文档可参考`command`目录下的[README](https://github.com/Tencent/VasDolly/blob/master/command/README.md)。

### 读取渠道信息
通过helper类库中的`ChannelReaderUtil`类读取渠道信息。
``` java
String channel = ChannelReaderUtil.getChannel(getApplicationContext());
```
如果没有渠道信息，那么这里返回`null`，开发者需要自己判断。

# Demo参考
详细的接入范式，可参考[Demo](https://github.com/Tencent/VasDolly/tree/master/app)

# 实现原理
具体原理可参考[VasDolly实现原理](https://github.com/Tencent/VasDolly/wiki/VasDolly%E5%AE%9E%E7%8E%B0%E5%8E%9F%E7%90%86)

# 问题反馈
遇到任何问题或者有好的建议，欢迎提[issues](https://github.com/Tencent/VasDolly/issues)，或者QQ（1031747903）联系。

# TODO

1. 增加单元测试
2. 防渠道信息篡改
3. 提供Python脚本

# License
VasDolly is under the BSD license. See the [LICENSE](https://github.com/Tencent/VasDolly/blob/master/LICENSE) file for details.
