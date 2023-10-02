package com.example.authtest.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.example.authtest.data.entities.Image

@Dao
interface Dao {

    @Query("SELECT * FROM Image ORDER BY timeStamp DESC")
    fun getImagesPagingSource() : PagingSource<Int, Image>

    @Upsert
    suspend fun insertNewImage(image : Image)

    @Query("DELETE FROM Image WHERE timeStamp =:imageId")
    suspend fun deleteImage(imageId: Long)

}