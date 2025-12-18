package ru.kaed.fishing.link.detector.core.data.repository

import android.util.Base64
import android.util.Patterns
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import ru.kaed.fishing.link.detector.core.data.dto.VirusTotalResponse
import ru.kaed.fishing.link.detector.core.domain.model.ChainLink
import ru.kaed.fishing.link.detector.core.domain.model.RiskLevel
import ru.kaed.fishing.link.detector.core.domain.model.UrlAnalysisResult
import ru.kaed.fishing.link.detector.core.domain.repository.UrlSecurityRepository
import java.net.URI

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

    override suspend fun analyzeUrl(input: String): UrlAnalysisResult {
        val isUrl = Patterns.WEB_URL.matcher(input).matches()

        if (!isUrl) {
            return UrlAnalysisResult(
                content = input,
                finalUrl = null,
                riskLevel = RiskLevel.INFO,
                details = "Это текстовые данные, не ссылка.",
                isUrl = false
            )
        }

        val (finalUrl, chain) = unshortenUrl(input)

        val vtResult = checkVirusTotal(finalUrl)

        var finalPageTitle: String? = null
        if (vtResult.risk != RiskLevel.DANGEROUS) {
            finalPageTitle = fetchPageTitle(finalUrl)
        }

        return UrlAnalysisResult(
            content = input,
            finalUrl = finalUrl,
            redirectChain = chain,
            riskLevel = vtResult.risk,
            details = vtResult.message,
            communityScore = vtResult.score,
            maliciousCount = vtResult.maliciousCount,
            title = finalPageTitle,
            isUrl = true
        )
    }

    private suspend fun unshortenUrl(initialUrl: String): Pair<String, List<ChainLink>> {
        val chain = mutableListOf<ChainLink>()
        var currentUrl = initialUrl

        chain.add(ChainLink(url = currentUrl, status = "Исходная ссылка"))

        var limit = 8
        while (limit > 0) {
            try {

                val response = client.head(currentUrl)
                val status = response.status.value

                if (status in 300..399) {
                    val location = response.headers["Location"]
                    if (location != null) {
                        val lastIndex = chain.lastIndex
                        if (lastIndex >= 0) {

                            chain[lastIndex] = chain[lastIndex].copy(status = "HTTP $status (Redirect)")
                        }


                        currentUrl = resolveRelativeUrl(currentUrl, location)


                        chain.add(ChainLink(url = currentUrl, status = "Переход..."))
                        limit--
                    } else {
                        break
                    }
                } else {

                    val lastIndex = chain.lastIndex
                    if (lastIndex >= 0) {
                        chain[lastIndex] = chain[lastIndex].copy(status = "HTTP $status (Final)")
                    }
                    break
                }
            } catch (e: Exception) {
                val lastIndex = chain.lastIndex
                if (lastIndex >= 0) {
                    chain[lastIndex] = chain[lastIndex].copy(status = "Ошибка доступа")
                }
                break
            }
        }
        return currentUrl to chain
    }

    private data class VtCheckResult(val risk: RiskLevel, val message: String, val score: Int, val maliciousCount: Int)

    private suspend fun checkVirusTotal(url: String): VtCheckResult {
        val urlId = Base64.encodeToString(url.toByteArray(), Base64.NO_PADDING or Base64.NO_WRAP).trim()
        val apiUrl = "https://www.virustotal.com/api/v3/urls/$urlId"

        return try {
            val response: HttpResponse = client.get(apiUrl) {
                header("x-apikey", apiKey)
            }

            if (response.status.value == 401) return VtCheckResult(RiskLevel.UNKNOWN, "Ошибка API ключа", 0, 0)
            if (response.status.value == 404) return VtCheckResult(RiskLevel.SAFE, "Новый URL (Нет в базе)", 0, 0)

            val body = response.bodyAsText()
            val vtResponse = jsonParser.decodeFromString<VirusTotalResponse>(body)
            val stats = vtResponse.data?.attributes?.stats
            val reputation = vtResponse.data?.attributes?.reputation ?: 0

            if (stats != null) {
                val malicious = stats.malicious
                val suspicious = stats.suspicious
                val totalBad = malicious + suspicious

                val (risk, msg) = when {
                    totalBad >= 3 -> {
                        RiskLevel.DANGEROUS to "Критическая угроза! ($totalBad детектов)"
                    }

                    totalBad in 1..2 && reputation >= 15 -> {
                        RiskLevel.SAFE to "Надежный сайт (Ложный детект игнорирован)"
                    }

                    reputation >= 50 -> {
                        RiskLevel.SAFE to "Высокое доверие сообщества"
                    }

                    totalBad >= 1 -> {
                        RiskLevel.DANGEROUS to "Обнаружена угроза"
                    }

                    reputation < -5 -> {
                        RiskLevel.SUSPICIOUS to "Плохая репутация сообщества"
                    }

                    else -> {
                        RiskLevel.SAFE to "Веб-сайт чист"
                    }
                }

                VtCheckResult(risk, msg, reputation, malicious)
            } else {
                VtCheckResult(RiskLevel.UNKNOWN, "Нет данных статистики", 0, 0)
            }
        } catch (e: Exception) {
            VtCheckResult(RiskLevel.UNKNOWN, "Ошибка проверки: ${e.message}", 0, 0)
        }
    }

    private suspend fun fetchPageTitle(url: String): String? {
        return try {
            val response = client.get(url)
            if (response.status.value == 200) {
                val text = response.bodyAsText()
                val regex = Regex("(?s)<title>(.*?)</title>", RegexOption.IGNORE_CASE)
                val match = regex.find(text)
                match?.groupValues?.get(1)?.trim()?.replace(Regex("\\s+"), " ")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun resolveRelativeUrl(baseUrl: String, location: String): String {
        return try {
            val baseUri = URI(baseUrl)
            baseUri.resolve(location).toString()
        } catch (e: Exception) {
            location
        }
    }
}