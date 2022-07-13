import com.google.gson.Gson
import io.netty.buffer.ByteBuf
import ru.cristalix.clientapi.KotlinMod
import ru.cristalix.uiengine.UIEngine
import ru.cristalix.uiengine.utility.Color
import java.nio.charset.StandardCharsets

lateinit var mod: App
lateinit var compass: Compass
var compassGui: CompassGui? = null
var playerData: PlayerData? = null

class App : KotlinMod() {

    fun openCompass() {
        compassGui?.openned = true
        compassGui?.search?.contentText?.content = ""
        compassGui?.search?.typing = true
        compassGui?.redraw()
        compassGui?.open()
    }

    var block = false

    override fun onEnable() {
        UIEngine.initialize(this)
        mod = this

        val gson = Gson()

        fun open() {
            block = true
            UIEngine.schedule(1) { block = false }

            if (compassGui == null) UIEngine.clientApi.chat().sendChatMessage("/hc-get-games")
            else {
                UIEngine.clientApi.chat().sendChatMessage("/hc-get-online")
                openCompass()
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
            compass = gson.fromJson(readString(), Compass::class.java)

            if (compassGui == null) compassGui = CompassGui(compass)

            compass.games.forEach { game ->
                game.subGames?.forEach { sub ->
                    sub.parent = game
                    sub.backgroundColor = game.backgroundColor
                    sub.icon = game.icon
                }

                println(game.depend)

                load(game.icon ?: return@forEach).thenAccept { location ->
                    val realm = game.realmType

                    compassGui?.games?.find {
                        (it.compassGame.depend == null && it.compassGame.realmType == realm)
                                || (it.compassGame.depend == realm)
                    }?.let {
                        it.compassGame.image = location
                        it.image.textureLocation = location
                    }
                }
            }

            openCompass()
        }


        registerChannel("hc:player") {
            playerData = gson.fromJson(readString(), PlayerData::class.java)
            compassGui?.redraw()
        }

        registerChannel("hc:online") {
            if (compassGui == null) return@registerChannel

            compass.games.forEach { game ->
                readString()
                    .replace("\"", "")
                    .split(",")
                    .map { it.split(":") }
                    .forEach { entry ->
                        if ((game.depend == null && game.realmType == entry[0]) || game.depend == entry[0])
                            game.online = entry[1].toInt()
                    }
            }
        }

        registerChannel("hc:games-by-type") {
            val data = readString()
            val game = gson.fromJson(data, SubGames::class.java)

            compassGui?.games?.find { it.compassGame.realmType == game.realmType }?.let {
                it.compassGame.subGames = game.games?.toList()?.onEach { current ->
                    current.parent = it.compassGame
                    current.image = it.image.textureLocation ?: loading
                    current.backgroundColor = it.compassGame.backgroundColor
                }

                SubCompassGui(it.compassGame).open()
            }
        }
    }

    fun hexToColor(hex: Int) = Color(hex and 0xFF0000 shr 16, hex and 0xFF00 shr 8, hex and 0xFF)

    fun join(realm: String) = UIEngine.clientApi.chat().sendChatMessage("/hc-join-to $realm")

}
