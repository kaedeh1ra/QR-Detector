package ru.kaed.fishing.link.detector.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.kaed.fishing.link.detector.BuildConfig
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Named("VirusTotalKey")
    fun provideApiKey(): String {
        return BuildConfig.VIRUSTOTAL_API_KEY
    }
}