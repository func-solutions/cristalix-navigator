import dev.xdark.clientapi.resource.ResourceLocation
import ru.cristalix.uiengine.UIEngine
import ru.cristalix.uiengine.UIEngine.clientApi
import ru.cristalix.uiengine.utility.*
import javax.swing.text.StyleConstants.getBackground

data class CompassGame(
    val name: String? = null,
    val title: String? = null,
    var icon: String? = null,
    var category: String? = null,
    val realmType: String? = null,
    var lobby: Boolean = false,
    var subGames: List<CompassGame>? = null,
    val tags: List<String>? = listOf(),
    var infoTag: String? = null,
    var infoDateBefore: String = "",
    var infoColor: Int = 10493224,
    var newActive: Boolean = false,
    var newDateBefore: String = "",
    val keywords: Set<String>? = setOf(),
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
        carved {
            val content = +text {
                content = it.tag
                align = BOTTOM
                origin = BOTTOM
                offset = V3(0.0, -1.5)
            }

            size = V3(clientApi.fontRenderer().getStringWidth(content.content) + 8.0, 12.0)
            color = it.getBackground()
        }
    }
}
