package ru.kaed.fishing.link.detector.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VirusTotalResponse(
    val data: VtData? = null,
    val error: VtError? = null
)

@Serializable
data class VtData(
    val attributes: VtAttributes
)

@Serializable
data class VtAttributes(
    @SerialName("last_analysis_stats")
    val stats: VtStats,
    val reputation: Int = 0
)

@Serializable
data class VtStats(
    val malicious: Int,
    val suspicious: Int,
    val harmless: Int,
    val undetected: Int
)

@Serializable
data class VtError(
    val message: String
)