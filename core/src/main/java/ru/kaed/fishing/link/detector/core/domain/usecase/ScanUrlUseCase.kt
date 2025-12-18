package ru.kaed.fishing.link.detector.core.domain.usecase

import ru.kaed.fishing.link.detector.core.domain.model.UrlAnalysisResult
import ru.kaed.fishing.link.detector.core.domain.repository.UrlSecurityRepository

class ScanUrlUseCase(private val repository: UrlSecurityRepository) {
    suspend operator fun invoke(url: String): UrlAnalysisResult {
        return repository.analyzeUrl(url)
    }
}