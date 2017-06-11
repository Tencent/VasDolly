# 签名校验模块

该模块主要负责校验生成的多渠道包能否在所有Android系统上正确安装。
主要依赖于Google的校验工具--[apksig](https://android.googlesource.com/platform/tools/apksig)进行校验。

---

这里单独提出一个Java Module，是因为`apksig`是通过Java8编译，和Android Module不兼容，目前还没找到好的解决方案。
可参考[Android: Dex cannot parse version 52 byte code](https://stackoverflow.com/questions/37020413/android-dex-cannot-parse-version-52-byte-code)