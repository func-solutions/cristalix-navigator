import com.google.gson.Gson
import dev.xdark.clientapi.event.chat.ChatSend
import element.DropElement
import io.netty.buffer.ByteBuf
import ru.cristalix.clientapi.KotlinMod
import ru.cristalix.uiengine.UIEngine.initialize
import ru.cristalix.uiengine.UIEngine.schedule
import ru.cristalix.uiengine.utility.Color
import java.nio.charset.StandardCharsets

lateinit var mod: App
lateinit var compass: Compass

var compassGui: CompassGui? = null
var playerData: PlayerData? = null

var compassUpdateId: Int = 0
var compassUpdate = false

inline fun drop(contentHeader: String, contentSelect: List<String>, action: DropElement.() -> Unit) = DropElement(contentHeader, contentSelect).also(action)

class App : KotlinMod() {

    companion object {
        fun gui() = compassGui
    }

    fun openCompass() {
        compassGui?.openned = true
        compassGui?.search?.contentText?.content = ""
        compassGui?.search?.typing = true

        val banners = compassGui?.banners()

        if (banners?.isNotEmpty() == true) {
            compassGui?.selectBanner = compassGui?.selectBanner!! + 1
            if (compassGui?.selectBanner == banners.size) compassGui?.selectBanner = 0

            var timeSlider = banners[compassGui?.selectBanner!!].timeSlider
            if (timeSlider < 5) timeSlider = 5

            compassGui?.timerOldBanner = (System.currentTimeMillis() / 1000) + timeSlider
        }

        compassGui?.redraw()
        compassGui?.open()
    }

    var block = false
    var stop = false

    override fun onEnable() {
        initialize(this)

        mod = this

        val gson = Gson()

        fun open() {
            if (stop) return
            block = true

            schedule(0.2) { block = false }

            if (compassGui == null || compassUpdate) {
                clientApi.chat().sendChatMessage("/hc-get-games")
            } else {
                openCompass()
                clientApi.chat().sendChatMessage("/hc-get-online")
            }
        }

        registerHandler<ChatSend> {
            if (message.startsWith("/func:navigator-stop")) {
                compassGui = null
                stop = true
            }
        }

        //registerHandler<KeyPress> { if (key == Keyboard.KEY_GRAVE) open() }
        registerChannel("func:navigator") { open() }

        fun ByteBuf.readString(): String {
            val buffer = ByteArray(readableBytes())
            readBytes(buffer)
            return String(buffer, StandardCharsets.UTF_8)
        }

        registerChannel("hc:games") {
            if (stop) return@registerChannel

            val saveData = playerData

            compass = gson.fromJson(readString(), Compass::class.java)

            if (compassGui == null) compassGui = CompassGui(compass)
            if (compass.globalOnline != null) compassGui?.totalOnline = compass.globalOnline!!

            compass.categories.forEach { cgc ->
                val name = cgc.name!!
                val displayName = cgc.displayName!!

                compassGui!!.categories[name] = Category(
                    displayName,
                    "Ой...\n\nА где все игры?"
                ) {
                    compass.games
                        .filter { it.category == name }
                        .sortedBy { -it.online }
                }
            }

            compass.games
                .forEach { game ->
                    game.subGames?.forEach { sub ->
                        sub.parent = game
                        sub.backgroundColor = game.backgroundColor
                        sub.icon = game.icon
                    }

                    load(game.icon ?: return@forEach).thenAccept { location ->
                        compassGui?.games?.filter { it.compassGame.icon == game.icon }?.forEach {
                            it.compassGame.image = location
                            it.image.textureLocation = location
                        }
                    }
            }

            clientApi.chat().sendChatMessage("/hc-get-online")

            if (compassUpdate) {
                playerData = saveData
                compassUpdate = false
            } else openCompass()
        }

        registerChannel("hc:player") {
            if (stop) return@registerChannel

            playerData = gson.fromJson(readString(), PlayerData::class.java)
            compassGui?.redraw()
        }

        registerChannel("hc:online") {
            if (compassGui == null || stop) return@registerChannel

            val data = gson.fromJson(readString(), Map::class.java)

            val online = data.getOrDefault("GLOBAL", 0.0)

            compass.games.forEach { game ->
                var gameOnline = (data.getOrDefault(game.realmType, 0.0) as Number).toInt()
                if (!game.depend.isNullOrEmpty()) gameOnline = (data.getOrDefault(game.depend, 0.0) as Number).toInt()

                game.online = gameOnline
            }

            compassGui?.totalOnline = (online as Number).toInt()

            val needUpdateId = (data.getOrDefault("COMPASS_UPDATE", 0.0) as Number).toInt()

            if (compassUpdateId == 0) {
                compassUpdateId = needUpdateId
                return@registerChannel
            }

            if (needUpdateId != compassUpdateId) {
                clientApi.chat().sendChatMessage("/hc-get-games")
                compassUpdateId = needUpdateId
                compassUpdate = true
            }
        }

        registerChannel("hc:games-by-type") {
            if (stop) return@registerChannel

            val game = gson.fromJson(readString(), SubGames::class.java)

            compassGui?.games?.find { it.compassGame.realmType == game.realmType }?.let {
                it.compassGame.subGames = game.games?.sortedBy { it.realmType }?.onEach { current ->
                    current.parent = it.compassGame
                    current.image = it.image.textureLocation ?: loading
                    current.backgroundColor = it.compassGame.backgroundColor
                }

                SubCompassGui(it.compassGame).open()
            }
        }
    }

    fun hexToColor(hex: Int) = Color(hex and 0xFF0000 shr 16, hex and 0xFF00 shr 8, hex and 0xFF)

    fun join(game: CompassGame) = clientApi.chat()
        .sendChatMessage("/hc-join-to ${if (game.parent == null) game.name ?: "HUB" else game.realmType ?: "HUB"}")
}