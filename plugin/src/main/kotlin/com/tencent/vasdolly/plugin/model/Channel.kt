package com.tencent.vasdolly.plugin.model

/***
 * VasDolly插件 渠道信息
 * https://developer.android.com/studio/build/extend-agp
 */
data class Channel(
    //渠道
    val channel: String,
    //渠道别名
    val alias: String?
)