import com.github.ikun.utils.FileHandler
import java.nio.file.Files
import kotlin.test.*

class FileHandlerTest {
    @Test
    fun `repeated config saves replace the file preserve UTF8 and clean up the temporary file`() {
        val folder = Files.createTempDirectory("ikun-config-test-").toFile()
        val file = folder.resolve("config.json")
        val temporary = folder.resolve("config.json.tmp")
        try {
            file.writeText("previous config")
            val handler = FileHandler(file)
            for (value in listOf("{\"enabled\":true}", "{\"name\":\"设置\",\"autoCannonClose\":true}")) {
                handler.write(value)
                assertEquals(value, file.readText())
                assertFalse(temporary.exists())
            }
        } finally {
            temporary.delete()
            file.delete()
            folder.delete()
        }
    }
}
