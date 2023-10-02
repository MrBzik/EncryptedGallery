package com.example.authtest.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Image(
    @PrimaryKey(autoGenerate = false)
    val timeStamp: Long,
    val path : String
)
