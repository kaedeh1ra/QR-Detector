package ru.kaed.fishing.link.core.detector.domain.repository

import ru.kaed.fishing.link.core.detector.domain.model.UrlAnalysisResult

interface UrlSecurityRepository {
    suspend fun analyzeUrl(url: String): UrlAnalysisResult
}