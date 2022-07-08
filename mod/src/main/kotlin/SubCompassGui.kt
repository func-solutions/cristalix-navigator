import org.lwjgl.input.Keyboard
import ru.cristalix.uiengine.element.ContextGui
import ru.cristalix.uiengine.eventloop.animate
import ru.cristalix.uiengine.onMouseUp
import ru.cristalix.uiengine.utility.*

class SubCompassGui(var parent: CompassGame) : ContextGui() {

    val scaling = 0.5

    val content = flex {
        align = CENTER
        origin = CENTER
        flexSpacing = 4.0

        parent.subGames?.forEach { sub ->
            +CompassNode(sub).apply {
                game.size = V3(160.0 * scaling, 212.0 * scaling)
                hint.size = game.size
                image.size = V3(148.0 * scaling, 148.0 * scaling)
                image.textureLocation = if (sub.parent != null && image.textureLocation == loading) sub.parent?.image
                else sub.image

                contentBox.size.y = 212.0 * scaling - 148.0 * scaling
                contentBox.size.x = 160.0 * scaling
                contentBox.offset.y += 148.0 * scaling
            }.game
        }
    }

    val container = +rectangle {
        align = CENTER
        origin = CENTER
        size = V3(400.0, 220.0)
        +text {
            align = TOP
            origin = TOP
            scale = V3(2.0, 2.0, 2.0)
            shadow = true
            content = parent.title ?: "Выберите подрежим"
        }
        val backButtonSize = 20.0
        +content
        +carved {
            carveSize = 1.0
            align = BOTTOM
            origin = BOTTOM

            size = V3(76.0, backButtonSize)
            val normalColor = Color(160, 29, 40, 0.83)
            val hoveredColor = Color(231, 61, 75, 0.83)
            color = normalColor
            onHover {
                animate(0.08, Easings.QUINT_OUT) {
                    color = if (hovered) hoveredColor else normalColor
                    scale = V3(if (hovered) 1.1 else 1.0, if (hovered) 1.1 else 1.0, 1.0)
                }
            }
            onMouseUp { mod.openCompass() }
            +text {
                align = CENTER
                origin = CENTER
                color = WHITE
                scale = V3(0.9, 0.9, 0.9)
                content = "Назад [ ESC ]"
                shadow = true
            }
        }
    }

    init {
        color = Color(0, 0, 0, 0.86)

        onKeyTyped { _, code ->
            if (code == Keyboard.KEY_ESCAPE) mod.openCompass()
        }
    }

}