import dev.xdark.clientapi.resource.ResourceLocation
import ru.cristalix.uiengine.UIEngine
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO

class RemoteTexture(
    @JvmField val location: ResourceLocation,
    @JvmField val sha1: String,
)

val NAMESPACE = "cache/animation"
private val cacheDir = Paths.get("$NAMESPACE/")

fun loadTexture(urlString: String, info: RemoteTexture): CompletableFuture<Void> =
    CompletableFuture.runAsync {
        try {
            val cacheDir = cacheDir
            Files.createDirectories(cacheDir)
            val path = cacheDir.resolve(info.sha1)

            val image = try {
                Files.newInputStream(path).use {
                    ImageIO.read(it)
                }
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
                return@execute
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

fun load(url: String): CompletableFuture<Void> =
    loadTexture(url, RemoteTexture(ResourceLocation.of(NAMESPACE, url.split("/").last()), "FUNC${url.hashCode()}FUNC"))
