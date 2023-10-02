package com.example.authtest.dagger

import android.content.Context
import androidx.room.Room
import com.example.authtest.data.ImagesDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext app : Context) =
        Room.databaseBuilder(
            app,
            ImagesDatabase::class.java,
            "images_db"
        ).build()


}