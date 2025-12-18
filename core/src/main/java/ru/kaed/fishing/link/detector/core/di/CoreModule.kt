package ru.kaed.fishing.link.detector.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.kaed.fishing.link.detector.core.data.repository.UrlSecurityRepositoryImpl
import ru.kaed.fishing.link.detector.core.domain.repository.UrlSecurityRepository
import ru.kaed.fishing.link.detector.core.domain.usecase.ScanUrlUseCase
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideRepository(
        @Named("VirusTotalKey") apiKey: String
    ): UrlSecurityRepository {
        return UrlSecurityRepositoryImpl(apiKey)
    }

    @Provides
    @Singleton
    fun provideScanUseCase(repository: UrlSecurityRepository): ScanUrlUseCase {
        return ScanUrlUseCase(repository)
    }
}