import ru.cristalix.uiengine.UIEngine.clientApi
import ru.cristalix.uiengine.onMouseUp
import ru.cristalix.uiengine.utility.*

class Category(val title: String, val empty: String, val games: () -> List<CompassGame>) {

    var hint = text {
        content = title
        origin = CENTER
        align = CENTER
    }

    val button = carved {
        carveSize = 2.0
        size = V3(clientApi.fontRenderer().getStringWidth(title) + 14.0, 19.0)
        color = Color(42, 102, 189, 1.0)

        +hint
    }
}
