import dev.xdark.clientapi.opengl.GlStateManager
import ru.cristalix.uiengine.UIEngine
import ru.cristalix.uiengine.UIEngine.clientApi
import ru.cristalix.uiengine.element.CarvedRectangle
import ru.cristalix.uiengine.element.RectangleElement
import ru.cristalix.uiengine.element.TextElement
import ru.cristalix.uiengine.eventloop.animate
import ru.cristalix.uiengine.onMouseUp
import ru.cristalix.uiengine.utility.*

class CompassNode(var compassGame: CompassGame) {

    val content = text {
        align = CENTER
        origin = CENTER
        offset.y -= 8
        content = compassGame.title ?: "Ошибка"
    }
    val online = text {
        align = CENTER
        origin = CENTER
        lineHeight += 1
        color = Color(255, 255, 255, 0.62)
        content = "Онлайн ${compassGame.online}"
    }
    val contentBox = rectangle {
        align = TOP
        origin = TOP
        +online
        online.offset.y += 4
        +content
    }
    val image = rectangle {
        align = TOP
        origin = TOP
        color = WHITE
        textureLocation = loading
    }
    lateinit var hint: CarvedRectangle
    lateinit var hintText: TextElement
    private lateinit var star: RectangleElement

    val game = carved {
        color = mod.hexToColor(compassGame.backgroundColor)
        carveSize = 2.0
        +image
        +contentBox
        hint = +carved {
            carveSize = 2.0
            color = Color(0, 0, 0, 0.0)
            align = CENTER
            origin = CENTER
            beforeRender { GlStateManager.disableDepth() }
            afterRender { GlStateManager.enableDepth() }
            hintText = +text {
                align = CENTER
                origin = CENTER
                color = WHITE
                offset.y += 6
                content = "ИГРАТЬ"
                enabled = false
            }

            onMouseUp {
                if (button != MouseButton.LEFT || mod.block) return@onMouseUp
                if (compassGame.subGames?.isEmpty() != false || compassGame.realmType == "HUB") mod.join(compassGame)
                else clientApi.chat().sendChatMessage("/hc-get-games-by-type " + compassGame.realmType)
            }

            if (compassGame.parent != null)
                return@carved
            star = +rectangle {
                align = TOP
                origin = TOP
                offset.y += 8
                textureLocation = starIcon
                enabled = false
                size = V3(11.0, 10.5)
                color = if (compassGame.starred) Color(255, 215, 0, 1.0) else WHITE
                onMouseUp {
                    if (compassGame.starred) {
                        compassGame.starred = false
                        clientApi.chat().sendChatMessage("/hc-remove-favorite ${compassGame.realmType}")
                        compassGame.realmType?.let { playerData?.removeFavorite(compassGame.depend ?: it) }
                    } else {
                        compassGame.starred = true
                        clientApi.chat().sendChatMessage("/hc-add-favorite ${compassGame.realmType}")
                        compassGame.realmType?.let { playerData?.addFavorite(compassGame.depend ?: it) }
                    }
                    animate(0.1) {
                        color = if (compassGame.starred) Color(255, 215, 0, 1.0) else WHITE
                    }
                }
                onHover {
                    color.alpha = if (hovered) .62 else 1.0
                }
            }
        }
        var on = false
        var animation = false

        beforeRender {
            if (animation) return@beforeRender

            val onHeader = compassGui?.header?.hovered == true
            if ((onHeader && on) || !hovered) {
                on = false
                hint.animate(0.1, Easings.CUBIC_OUT) { color.alpha = 0.0 }
            } else if (!on && hovered && !onHeader) {
                on = true
                hint.animate(0.1, Easings.CUBIC_OUT) { color.alpha = 0.82 }
            } else return@beforeRender

            animation = true

            UIEngine.schedule(0.1001) { animation = false }
            hintText.enabled = on
            if (compassGame.parent != null)
                return@beforeRender
            star.enabled = on
        }
    }
}

