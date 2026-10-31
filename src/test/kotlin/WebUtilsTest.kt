import com.github.ikun.utils.kuudra.KuudraPartyData
import com.github.ikun.utils.network.WebUtils
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPOutputStream
import kotlin.test.*

class WebUtilsTest {
    private val profile = """{"playerName":"hu_nya","profileName":"Coconut","fetchedAt":123,"futureField":true,"hasHyperion":false}"""

    private fun withServer(handler: (HttpExchange) -> Unit, block: (String) -> Unit) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val executor = Executors.newVirtualThreadPerTaskExecutor()
        server.executor = executor
        server.createContext("/v1/kuudra/hu_nya") { exchange -> exchange.use { handler(it) } }
        server.start()
        try { block("http://127.0.0.1:${server.address.port}/v1/kuudra/hu_nya") }
        finally { server.stop(0); executor.close() }
    }

    @Test
    fun `plain gzip and deflate responses keep nullable gear and tolerate new fields`() {
        for (encoding in listOf("identity", "gzip", "deflate")) {
            val bytes = if (encoding == "identity") profile.toByteArray() else ByteArrayOutputStream().also { output ->
                val compressor = if (encoding == "gzip") GZIPOutputStream(output) else DeflaterOutputStream(output)
                compressor.use { it.write(profile.toByteArray()) }
            }.toByteArray()
            withServer({ exchange ->
                assertEquals("Ikun/1.0.0", exchange.requestHeaders.getFirst("User-Agent"))
                assertEquals("application/json", exchange.requestHeaders.getFirst("Accept"))
                exchange.responseHeaders.set("Content-Encoding", encoding)
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.write(bytes)
            }) { url ->
                val result = runBlocking { WebUtils.getAs<KuudraPartyData>(url).getOrThrow() }
                assertEquals("hu_nya", result.playerName)
                assertEquals(false, result.hasHyperion)
                assertNull(result.hasToxicArrowPoison)
                assertEquals(123L, result.fetchedAt)
            }
        }
    }

    @Test
    fun `HTTP errors and invalid JSON return failures instead of player data`() {
        for ((status, body) in listOf(503 to "unavailable", 200 to "<html>proxy error</html>", 200 to "{}")) {
            withServer({ exchange ->
                val bytes = body.toByteArray()
                exchange.sendResponseHeaders(status, bytes.size.toLong())
                exchange.responseBody.write(bytes)
            }) { url ->
                val result = runBlocking { WebUtils.getAs<KuudraPartyData>(url) }
                assertTrue(result.isFailure)
                if (status == 503) assertEquals("HTTP 503", result.exceptionOrNull()?.message)
            }
        }
    }

    @Test
    fun `cancelling a pending query exits before the server responds`() {
        val arrived = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        withServer({ _ ->
            arrived.complete(Unit)
            release.await()
        }) { url ->
            try {
                runBlocking {
                    withTimeout(5_000) {
                        val query = async { WebUtils.getAs<KuudraPartyData>(url) }
                        arrived.await()
                        query.cancelAndJoin()
                        assertTrue(query.isCancelled)
                    }
                }
            } finally { release.countDown() }
        }
    }
}
