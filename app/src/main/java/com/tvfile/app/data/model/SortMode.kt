package com.tvfile.app.data.model

enum class SortMode(val displayName: String) {
    TIME_DESC("时间 (最新优先)"),
    TIME_ASC("时间 (最早优先)"),
    NAME_ASC("名称 (A - Z)"),
    NAME_DESC("名称 (Z - A)"),
    SIZE_DESC("大小 (从大到小)"),
    SIZE_ASC("大小 (从小到大)")
}
