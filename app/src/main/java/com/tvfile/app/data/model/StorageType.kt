package com.tvfile.app.data.model

enum class StorageType(val displayName: String) {
    INTERNAL("内部存储"),
    USB("外接存储"),
    SMB("网络共享(SMB)"),
    WEBDAV("网络存储(WebDAV)")
}
