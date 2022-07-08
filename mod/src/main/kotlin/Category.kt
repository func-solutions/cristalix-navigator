import ru.cristalix.uiengine.UIEngine
import ru.cristalix.uiengine.onMouseUp
import ru.cristalix.uiengine.utility.*

class Category(title: String, val empty: String, val games: () -> List<CompassGame>) {
    private val padding = 5.0

    var hint = text {
        origin = CENTER
        align = CENTER
        content = title
        scale = V3(0.75, 0.75, 0.75)
    }
    val button = carved {
        color = Color(42, 102, 189, 1.0)
        size = V3(
            2 * padding + UIEngine.clientApi.fontRenderer().getStringWidth(title) * hint.scale.x,
            22.0 - 2 * padding,
            1.0
        )
        +hint
        onMouseUp {
            compassGui?.activeCategory = this@Category
            compassGui?.updateTagsState = true
            compassGui?.redraw()
        }
    }
}
