import dev.xdark.clientapi.event.render.ScaleChange
import dev.xdark.clientapi.event.window.WindowResize
import dev.xdark.clientapi.render.ScaledResolution
import org.lwjgl.input.Keyboard
import ru.cristalix.uiengine.UIEngine.clientApi
import ru.cristalix.uiengine.element.ContextGui
import ru.cristalix.uiengine.element.RectangleElement
import ru.cristalix.uiengine.eventloop.animate
import ru.cristalix.uiengine.onMouseUp
import ru.cristalix.uiengine.utility.*
import kotlin.math.floor
import kotlin.math.min
import kotlin.streams.toList

class SubCompassGui(var parent: CompassGame) : ContextGui() {

    private var rootElement: RectangleElement? = null

    private val flex = flex {
        align = CENTER
        origin = CENTER

        flexDirection = FlexDirection.RIGHT
        flexSpacing = 7.0

        overflowWrap = true

        val boxSizeX = 80.0
        val boxSizeY = 111.0

        val games = parent.subGames

        if (games != null) {
            val gameSize = games.size

            games.stream().limit(12).toList().map { sub ->
                val node = CompassNode(sub).apply {
                    image.textureLocation = if (sub.parent != null && image.textureLocation == loading) sub.parent?.image
                    else sub.image
                }

                +node.game
                return@map node
            }

            size = V3(
                min((6 * boxSizeX) + (5 * flexSpacing), (gameSize * boxSizeX) + ((gameSize - 1) * flexSpacing)),
                if (gameSize > 6) (boxSizeY * 2) + flexSpacing else boxSizeY
            )
        }
    }

    init {
        color = Color(0, 0, 0, 0.86)

        onKeyTyped { _, code ->
            if (code == Keyboard.KEY_ESCAPE) mod.openCompass()
        }

        update()
        updateScale()

        mod.registerHandler<WindowResize> { updateScale(resolution) }
        mod.registerHandler<ScaleChange> { updateScale() }
    }

    private fun update() {
        rootElement?.let { removeChild(it) }

        rootElement = +rectangle {

            origin = CENTER
            align = CENTER

            +text {
                content = parent.title ?: "Игры"
                align = TOP
                origin = TOP
                scale = V3(2.0, 2.0)
            }

            +text {
                content = "Выбор подрежима"
                align = TOP
                origin = TOP
                offset = V3(0.0, 31.0)
            }

            +flex

            val contentExit = "Назад [ESC]"

            val sizeX = clientApi.fontRenderer().getStringWidth(contentExit) + 14.0
            val sizeY = 19.0

            +rectangle {
                size = V3(sizeX, sizeY)
                align = BOTTOM
                origin = BOTTOM

                +carved {
                    carveSize = 2.0
                    size = V3(sizeX, sizeY)
                    align = CENTER
                    origin = CENTER

                    val normalColor = Color(42, 102, 189)
                    val hoveredColor = Color(74, 140, 236)

                    color = normalColor

                    onHover {
                        animate(0.228, Easings.CIRC_OUT) {
                            color = if (hovered) hoveredColor else normalColor
                            scale = if (hovered) V3(1.025, 1.025) else V3(1.0, 1.0)
                        }
                    }

                    onMouseUp { mod.openCompass() }
                }

                +text {
                    content = contentExit
                    align = CENTER
                    origin = CENTER
                    color = Color(255, 255, 255)
                    scale = V3(1.0, 1.0, 1.0)
                }
            }

            size = V3(flex.size.x, 153.5 + flex.size.y)
        }
    }

    fun updateScale(resolution: ScaledResolution = clientApi.resolution()) {
        val rootElement = rootElement ?: return

        val factor = resolution.scaleFactor

        val scaleWidth = resolution.scaledWidth_double
        val scaleHeight = resolution.scaledHeight_double

        val sizeX = scaleWidth * factor / 2.0
        val sizeY = scaleHeight * factor / 2.0

        val highX = (sizeY / sizeX) * 100.0
        val highY = (sizeX / sizeY) * 100.0

        val scaleX = if (highX <= 100.0) sizeY else sizeX
        val scaleY = if (highY <= 100.0) sizeX else sizeY

        val scaleXY = (scaleX + scaleY) / 2.0
        val sizeXY = (sizeX + sizeY) / 2.0

        var scaleTotal = (((if (highX <= 47.257 || highY <= 47.257) scaleXY else sizeXY) * 0.001) + 0.26775)

        scaleTotal = if (factor == 1) scaleTotal * 2.0 else if (factor == 2) scaleTotal else scaleTotal / (0.5 * factor)

        val rootSizeX = rootElement.size.x + 30.0
        val rootSizeY = rootElement.size.y + 30.0

        val percentX = (sizeX / rootSizeX) * 100.0
        val percentY = (sizeY / rootSizeY) * 100.0

        val scX = (1.0 * percentX) / 100.0
        val scY = (1.0 * percentY) / 100.0

        if ((sizeX <= rootSizeX || sizeY <= rootSizeY) && factor == 1) scaleTotal /= 1.73225 - (if (sizeX <= rootSizeX) scX else scY)

        scaleTotal = (floor(scaleTotal * 10.0) / 10.0)
        scaleTotal = if (scaleTotal == 1.0) scaleTotal else scaleTotal + 0.125

        size = V3(sizeX, sizeY)

        rootElement.scale = V3(scaleTotal, scaleTotal, 1.0)
    }
}