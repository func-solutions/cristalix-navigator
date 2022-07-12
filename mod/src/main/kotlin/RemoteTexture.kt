import dev.xdark.clientapi.resource.ResourceLocation
import ru.cristalix.uiengine.UIEngine
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO
import kotlin.experimental.and

class RemoteTexture(
    @JvmField val location: ResourceLocation,
    @JvmField val sha1: String,
)

val NAMESPACE = "cache/animation"
private val cacheDir = Paths.get("$NAMESPACE/")

fun loadTexture(urlString: String, info: RemoteTexture): CompletableFuture<ResourceLocation> {
    val future = CompletableFuture<ResourceLocation>()
    CompletableFuture.runAsync {
        try {
            val cacheDir = cacheDir
            Files.createDirectories(cacheDir)
            val path = cacheDir.resolve(info.sha1)

            val image = try {
                Files.newInputStream(path).use { ImageIO.read(it) }
            } catch (ex: IOException) {
                val url = URL(urlString)
                val bytes = url.openStream().readBytes()
                Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
                ImageIO.read(ByteArrayInputStream(bytes))
            }
            val api = UIEngine.clientApi
            val mc = api.minecraft()
            val renderEngine = api.renderEngine()
            mc.execute {
                renderEngine.loadTexture(info.location, renderEngine.newImageTexture(image, false, false))
                future.complete(info.location)
            }
        } catch (e: Exception) {
            future.completeExceptionally(e)
        }
    }
    return future
}

fun load(url: String): CompletableFuture<ResourceLocation> =
    loadTexture(url, RemoteTexture(ResourceLocation.of(NAMESPACE, url.split("/").last()), getSha1Hex(url)))

fun getSha1Hex(clearString: String): String {
    return try {
        val messageDigest = MessageDigest.getInstance("SHA-1")
        messageDigest.update(clearString.toByteArray(charset("UTF-8")))
        StringBuilder().apply {
            messageDigest.digest().forEach { byte ->
                append(((byte and 0xff.toByte()) + 0x100).toString(16).substring(1))
            }
        }.toString()
    } catch (ignored: Exception) {
        ignored.printStackTrace()
        ""
    }
}