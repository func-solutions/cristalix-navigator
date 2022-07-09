import org.lwjgl.input.Keyboard
import ru.cristalix.uiengine.UIEngine
import ru.cristalix.uiengine.element.ContextGui
import ru.cristalix.uiengine.eventloop.animate
import ru.cristalix.uiengine.onMouseUp
import ru.cristalix.uiengine.utility.*

class SubCompassGui(var parent: CompassGame) : ContextGui() {

    val scalingBox = if (parent.subGames?.size!! > 4) 0.38 else 0.5

    val content = flex {
        align = CENTER
        origin = CENTER
        flexSpacing = 4.0

        if (parent.subGames?.size!! > 4)
            overflowWrap = true

        val boxSize = 160.0 * scalingBox

        beforeTransform {
            if (!overflowWrap) return@beforeTransform
            val box = UIEngine.overlayContext.size.x * 7 / 10
            val count = (box / boxSize).toInt()
            size.x = count * boxSize + (count - 1) * flexSpacing + 0.01
        }

        parent.subGames?.forEach { sub ->
            +CompassNode(sub).apply {
                game.size = V3(boxSize, 212.0 * scalingBox)
                hint.size = game.size
                image.size = V3(148.0 * scalingBox, 148.0 * scalingBox)
                image.textureLocation = if (sub.parent != null && image.textureLocation == loading) sub.parent?.image
                else sub.image

                if (overflowWrap) content.scale = V3(.5, .5, .5)

                contentBox.size.y = 212.0 * scalingBox - 148.0 * scalingBox
                contentBox.size.x = boxSize
                contentBox.offset.y += 148.0 * scalingBox
            }.game
        }
    }

    val container = +rectangle {
        align = CENTER
        origin = CENTER
        size = V3(400.0, 250.0)
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