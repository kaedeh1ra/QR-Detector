package ru.kaed.fishing.link.detector.core.domain.repository

import ru.kaed.fishing.link.detector.core.domain.model.UrlAnalysisResult

interface UrlSecurityRepository {
    suspend fun analyzeUrl(url: String): UrlAnalysisResult
}