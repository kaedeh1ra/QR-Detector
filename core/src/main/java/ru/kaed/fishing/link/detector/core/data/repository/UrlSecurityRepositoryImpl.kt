package ru.kaed.fishing.link.detector.core.data.repository

import android.util.Base64
import android.util.Log
import android.util.Patterns
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import ru.kaed.fishing.link.detector.core.domain.model.RiskLevel
import ru.kaed.fishing.link.detector.core.domain.model.UrlAnalysisResult
import ru.kaed.fishing.link.detector.core.domain.repository.UrlSecurityRepository
import ru.kaed.fishing.link.detector.core.data.dto.VirusTotalResponse

class UrlSecurityRepositoryImpl(
    private val apiKey: String
) : UrlSecurityRepository {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = HttpClient(Android) {
        followRedirects = false
    }

    override suspend fun analyzeUrl(url: String): UrlAnalysisResult {
        val isUrl = Patterns.WEB_URL.matcher(url).matches()

        if (!isUrl) {
            return UrlAnalysisResult(
                content = url,
                finalUrl = null,
                riskLevel = RiskLevel.INFO,
                details = "Это не ссылка, а текстовые данные.",
                isUrl = false
            )
        }

        val (finalUrl, chain) = unshortenUrl(url)
        val (risk, details) = checkVirusTotal(finalUrl)

        return UrlAnalysisResult(
            content = url,
            finalUrl = finalUrl,
            redirectChain = chain,
            riskLevel = risk,
            details = details,
            isUrl = true
        )
    }

    private suspend fun unshortenUrl(initialUrl: String): Pair<String, List<String>> {
        val chain = mutableListOf<String>()
        var currentUrl = initialUrl
        chain.add(currentUrl)

        var limit = 10
        while (limit > 0) {
            try {
                val response = client.head(currentUrl)
                if (response.status.value in 300..399) {
                    val location = response.headers["Location"]
                    if (location != null) {
                        currentUrl = if (location.startsWith("/")) {
                            initialUrl.substringBefore("/", "") + location
                        } else {
                            location
                        }
                        chain.add(currentUrl)
                        limit--
                    } else break
                } else break
            } catch (_: Exception) {
                break
            }
        }
        return currentUrl to chain
    }

    private suspend fun checkVirusTotal(url: String): Pair<RiskLevel, String> {
        val urlId = Base64.encodeToString(url.toByteArray(), Base64.NO_PADDING or Base64.NO_WRAP).trim()
        val apiUrl = "https://www.virustotal.com/api/v3/urls/$urlId"

        return try {
            val response: HttpResponse = client.get(apiUrl) {
                header("x-apikey", apiKey)
            }

            val responseBody = response.bodyAsText()

            if (response.status.value == 404) {
                return RiskLevel.UNKNOWN to "URL не найден в базе VirusTotal (возможно, новый)"
            }

            if (response.status.value == 401) {
                return RiskLevel.UNKNOWN to "Ошибка API ключа (401 Unauthorized)"
            }

            val vtResponse = jsonParser.decodeFromString<VirusTotalResponse>(responseBody)

            val stats = vtResponse.data?.attributes?.stats

            if (stats != null) {
                val score = stats.malicious + stats.suspicious
                when {
                    score >= 3 -> RiskLevel.DANGEROUS to "Критическая угроза! Детектов: $score"
                    score > 0 -> RiskLevel.SUSPICIOUS to "Подозрительный сайт. Детектов: $score"
                    else -> RiskLevel.SAFE to "Чисто. Детектов: 0"
                }
            } else {
                RiskLevel.UNKNOWN to "Ошибка чтения данных (stats is null)"
            }
        } catch (e: Exception) {
            Log.e("VirusTotal", "Error", e)
            RiskLevel.UNKNOWN to "Ошибка: ${e.message}"
        }
    }
}