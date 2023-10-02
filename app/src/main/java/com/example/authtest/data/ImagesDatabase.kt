package com.example.authtest.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.authtest.data.entities.Image

@Database(
    entities = [Image::class],
    version = 1
)
abstract class ImagesDatabase : RoomDatabase() {
    abstract val dao : Dao
}