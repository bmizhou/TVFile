package com.tvfile.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tvfile.app.data.model.StorageType

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val storageType: StorageType,
    val host: String,
    val port: Int,
    val username: String = "",
    val password: String = "",
    val isAnonymous: Boolean = false,
    val sharePath: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
