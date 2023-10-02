package com.example.authtest.presenter

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.net.toFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import com.example.authtest.data.ImagesDatabase
import com.example.authtest.data.entities.Image
import com.example.authtest.presenter.model.ImagePresent
import com.example.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ImagesViewModel @Inject constructor(
    private val db : ImagesDatabase,
    private val app : Application
) : AndroidViewModel(app) {


    private val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val cachedBitmaps = HashMap<Long, Bitmap>()


    private fun getImage(path : String) : Bitmap{

        val file = File(path)

        val encryptedFile = EncryptedFile.Builder(
            file,
            app,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileInput().use {
            return BitmapFactory.decodeStream(it)
        }
    }


    fun addImage(uri : Uri) = viewModelScope.launch{
        encryptAndAddImage(uri)
    }


    fun deleteImage(image : ImagePresent) = viewModelScope.launch{

        app.deleteFile(image.timestamp.toString())

        db.dao.deleteImage(imageId = image.timestamp)


    }


    private suspend fun encryptAndAddImage(uri : Uri){

        val timeStamp = System.currentTimeMillis()

        val encryptedFile = EncryptedFile.Builder(
            File(app.filesDir.absolutePath, timeStamp.toString()),
            app,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()


        app.contentResolver.openInputStream(uri)?.use { inputStream ->

            encryptedFile.openFileOutput().use {outputStream ->


                val buffer = ByteArray(4096)

                while (true){
                    val byteCount = inputStream.read(buffer)
                    if (byteCount < 0) break
                    outputStream.write(buffer, 0, byteCount)
                }

                outputStream.flush()

            }
        }



        db.dao.insertNewImage(
            Image(
                timeStamp = timeStamp,
                path = app.filesDir.absolutePath + "/" + timeStamp
            )
        )


        //DELETE ORIGINAL IMAGE FROM EXTERNAL STORAGE



        app.contentResolver.delete(uri, null, null)



    }


    fun imagesPagingFlow() : Flow<PagingData<ImagePresent>>{
       return Pager(
            config = PagingConfig(
                pageSize = 10,
                initialLoadSize = 10
            )
        ){
            db.dao.getImagesPagingSource()
        }.flow.map {

            it.map { image ->

                val bitmap = if(cachedBitmaps.contains(image.timeStamp)){
                    cachedBitmaps[image.timeStamp]
                } else {
                    val bit = getImage(image.path)
                    cachedBitmaps[image.timeStamp] = bit
                    bit
                }

                ImagePresent(image.timeStamp, bitmap)
            }
       }
    }






}