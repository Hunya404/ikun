package com.github.ikun.utils.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletionException
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

object WebUtils {
    private val client by lazy {
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
    }
    @PublishedApi internal val json = Json { ignoreUnknownKeys = true }

    @PublishedApi
    internal suspend fun getText(url: String): String {
        val request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(45))
            .header("User-Agent", "Ikun/1.0.0")
            .header("Accept", "application/json")
            .header("Accept-Encoding", "gzip, deflate")
            .GET().build()
        val response = suspendCancellableCoroutine<HttpResponse<ByteArray>> { continuation ->
            val future = client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
            continuation.invokeOnCancellation { future.cancel(true) }
            future.whenComplete { value, error ->
                if (error == null) continuation.resumeWith(Result.success(value))
                else continuation.resumeWith(Result.failure(
                    if (error is CompletionException) error.cause ?: error else error
                ))
            }
        }
        if (response.statusCode() !in 200..299) error("HTTP ${response.statusCode()}")
        val bytes = response.body()
        return when (val encoding = response.headers().firstValue("Content-Encoding").orElse("").lowercase()) {
            "", "identity" -> bytes.toString(Charsets.UTF_8)
            "gzip" -> GZIPInputStream(bytes.inputStream()).use { it.readAllBytes().toString(Charsets.UTF_8) }
            "deflate" -> InflaterInputStream(bytes.inputStream()).use { it.readAllBytes().toString(Charsets.UTF_8) }
            else -> error("Unsupported content encoding: $encoding")
        }
    }

    suspend inline fun <reified T> getAs(url: String): Result<T> = try {
        Result.success(json.decodeFromString<T>(getText(url)))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
