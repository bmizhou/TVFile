package com.tvfile.app.data.model

enum class SortMode(val displayName: String, val shortDisplayName: String) {
    TIME_DESC("修改时间 (最新优先)", "时间 ⬇ 最新"),
    TIME_ASC("修改时间 (最早优先)", "时间 ⬆ 最早"),
    NAME_ASC("文件名称 (A - Z)", "名称 A-Z"),
    NAME_DESC("文件名称 (Z - A)", "名称 Z-A"),
    SIZE_DESC("文件大小 (从大到小)", "大小 ⬇ 大到小"),
    SIZE_ASC("文件大小 (从小到大)", "大小 ⬆ 小到大")
}
