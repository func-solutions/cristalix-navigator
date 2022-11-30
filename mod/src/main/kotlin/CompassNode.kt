import App.Companion.gui
import dev.xdark.clientapi.opengl.GlStateManager
import ru.cristalix.uiengine.UIEngine.clientApi
import ru.cristalix.uiengine.element.CarvedRectangle
import ru.cristalix.uiengine.element.RectangleElement
import ru.cristalix.uiengine.element.TextElement
import ru.cristalix.uiengine.eventloop.animate
import ru.cristalix.uiengine.onMouseDown
import ru.cristalix.uiengine.onMouseUp
import ru.cristalix.uiengine.utility.*

class CompassNode(var compassGame: CompassGame) {

    val contentText = text {
        var titleContent = compassGame.title ?: "Ошибка"

        titleContent = titleContent.replace("§[0-9A-z]".toRegex(), "")
        titleContent = titleContent.replace("\n", " ")

        if (titleContent.contains(" ")) {
            var totalSize = 0.0
            val split = titleContent.split(" ")

            titleContent = ""

            split.forEach {
                val size = clientApi.fontRenderer().getStringWidth(it)

                if (totalSize + size >= 70.0 && titleContent.isNotEmpty()) {
                    totalSize = 0.0
                    titleContent += "\n$it"
                } else {
                    totalSize += size
                    titleContent += if (titleContent.isEmpty()) it else " $it"
                }
            }
        }

        content = titleContent
        align = BOTTOM
        origin = BOTTOM
        offset = V3(0.0, -21.0)
    }

    val online = text {
        content = "Онлайн ${compassGame.online}"
        align = BOTTOM
        origin = BOTTOM
        offset = V3(0.0, -8.0)
        color = Color(255, 255, 255, 0.62)
    }

    val contentInfo = text {
        content = compassGame.infoTag ?: ""
        align = CENTER
        origin = CENTER
    }

    val infoBox = carved {
        carveSize = 2.0
        size = V3(clientApi.fontRenderer().getStringWidth(contentInfo.content) + 16.0, 13.0)
        align = TOP
        origin = TOP
        offset = V3(0.0, -4.5)
        color = mod.hexToColor(compassGame.infoColor)

        +contentInfo

        enabled = false
    }

    val image = rectangle {
        size = V3(74.0, 74.0)
        align = TOP
        origin = TOP
        color = WHITE
        textureLocation = loading
    }

    lateinit var hint: CarvedRectangle
    lateinit var hintText: TextElement
    lateinit var star: RectangleElement

    val game = carved {
        carveSize = 3.0
        size = V3(80.0, 111.0)
        color = mod.hexToColor(compassGame.backgroundColor)

        +image

        +contentText
        +online

        hintText = text {
            align = CENTER
            origin = CENTER
            color = WHITE
            content = "ИГРАТЬ"
            scale = V3(1.5, 1.5)

            color.alpha = 0.0
        }

        star = rectangle {
            align = TOP
            origin = TOP
            offset.y = 10.0
            textureLocation = starIcon
            size = V3(11.0, 11.0)
            color = if (compassGame.starred) Color(255, 215, 0, 1.0) else WHITE
            color.alpha = 0.0

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
                animate(0.228, Easings.CIRC_OUT) {
                    color = if (compassGame.starred) Color(255, 215, 0, 1.0) else WHITE
                }
            }
        }

        hint = +carved {
            carveSize = 3.0
            align = CENTER
            origin = CENTER
            size = V3(80.0, 111.0)
            color = Color(0, 0, 0, 0.0)

            beforeRender { GlStateManager.disableDepth() }
            afterRender { GlStateManager.enableDepth() }

            onMouseDown {
                if (button != MouseButton.LEFT || mod.block) return@onMouseDown

                if (compassGui?.cdClick!! <= System.currentTimeMillis() / 1000) return@onMouseDown

                if (compassGame.realmType == "TEST") {

                    val gui = gui()

                    if (gui != null) {
                        gui.dropMenu?.headerText?.content = testCategory.title
                        gui.dropMenu?.content?.children?.forEach { it.color = Color(0, 0, 0, 0.0) }
                        gui.dropMenu?.content?.children?.last()?.color = Color(42, 102, 189)

                        gui.hoverContainer?.enabled = false
                    }

                    compassGui?.activeCategory = testCategory
                    compassGui?.updateTagsState = true
                    compassGui?.redraw()

                    return@onMouseDown
                }

                if (compassGame.subGames?.isEmpty() != false || compassGame.realmType == "HUB") mod.join(compassGame)
                else clientApi.chat().sendChatMessage("/hc-get-games-by-type " + compassGame.realmType)
            }

            +hintText

            if (compassGame.parent == null) {
                hintText.offset.y = 6.0
                +star
            } else hintText.offset.y = 0.0
        }

        +infoBox
        if (contentInfo.content.isNotEmpty()) infoBox.enabled = false
    }
}

