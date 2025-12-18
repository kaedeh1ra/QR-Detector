package ru.kaed.fishing.link.core.detector.domain.model

data class UrlAnalysisResult(
    val content: String,
    val finalUrl: String?,
    val redirectChain: List<String> = emptyList(),
    val riskLevel: RiskLevel,
    val details: String,
    val isUrl: Boolean
)

enum class RiskLevel {
    SAFE, SUSPICIOUS, DANGEROUS, UNKNOWN, INFO
}