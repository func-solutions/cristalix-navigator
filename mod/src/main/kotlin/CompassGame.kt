import dev.xdark.clientapi.resource.ResourceLocation
import ru.cristalix.uiengine.UIEngine
import ru.cristalix.uiengine.utility.CENTER
import ru.cristalix.uiengine.utility.V3
import ru.cristalix.uiengine.utility.carved
import ru.cristalix.uiengine.utility.text

data class CompassGame(
    val name: String? = null,
    val title: String? = null,
    var icon: String? = null,
    val realmType: String? = null,
    var lobby: Boolean = false,
    var subGames: List<CompassGame>? = null,
    val tags: List<String>? = null,
    var backgroundColor: Int = 0,
    var starred: Boolean = false,
    var dynamic: Boolean = false,
    var depend: String? = null,
    var image: ResourceLocation = loading,
    var description: List<String>? = listOf()
) {

    var parent: CompassGame? = null

    var online: Int = 0
        set(value) {
            compassGui?.games?.find { it.compassGame == this }?.online?.content = "Онлайн $value"
            field = value
        }

    fun createTags() = tags?.mapNotNull { key -> compass.tags.firstOrNull { it.tag == key } }?.map {
        val scaled = 0.75 + 0.125

        carved {
            val content = +text {
                align = CENTER
                origin = CENTER
                content = it.tag
                shadow = true
                scale = V3(scaled, scaled, scaled)
            }
            color = it.getBackground()
            size = V3(
                UIEngine.clientApi.fontRenderer().getStringWidth(content.content) * scaled + 6,
                content.lineHeight + 2
            )
        }
    }
}
