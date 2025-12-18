package ru.kaed.fishing.link.core.detector.domain.usecase

import ru.kaed.fishing.link.core.detector.domain.model.UrlAnalysisResult
import ru.kaed.fishing.link.core.detector.domain.repository.UrlSecurityRepository

class ScanUrlUseCase(private val repository: UrlSecurityRepository) {
    suspend operator fun invoke(url: String): UrlAnalysisResult {
        return repository.analyzeUrl(url)
    }
}