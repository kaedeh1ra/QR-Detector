package ru.kaed.fishing.link.detector.core.domain.model

data class UrlAnalysisResult(
    val content: String,
    val finalUrl: String?,
    val redirectChain: List<ChainLink> = emptyList(),
    val riskLevel: RiskLevel,
    val title: String? = null,
    val details: String,
    val communityScore: Int = 0,
    val maliciousCount: Int = 0,
    val isUrl: Boolean
)

data class ChainLink(
    val url: String,
    val title: String? = null,
    val status: String = "Link"
)

enum class RiskLevel {
    SAFE, SUSPICIOUS, DANGEROUS, UNKNOWN, INFO
}