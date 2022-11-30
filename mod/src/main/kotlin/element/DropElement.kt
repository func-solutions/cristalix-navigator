package element

import dev.xdark.clientapi.opengl.GlStateManager
import dev.xdark.clientapi.render.BufferBuilder
import dev.xdark.clientapi.render.DefaultVertexFormats
import dev.xdark.clientapi.render.Tessellator
import dev.xdark.clientapi.resource.ResourceLocation
import org.lwjgl.opengl.GL11
import ru.cristalix.clientapi.JavaMod
import ru.cristalix.uiengine.UIEngine
import ru.cristalix.uiengine.UIEngine.clientApi
import ru.cristalix.uiengine.element.RectangleElement
import ru.cristalix.uiengine.eventloop.animate
import ru.cristalix.uiengine.onMouseDown
import ru.cristalix.uiengine.utility.*

class DropElement(val contentHeader: String, val contentSelect: List<String>) : RectangleElement() {

    private val up: ResourceLocation = JavaMod.loadTextureFromJar(clientApi, "compass", "up", "icon/arrow_up.png")
    private val down: ResourceLocation = JavaMod.loadTextureFromJar(clientApi, "compass", "down", "icon/arrow_down.png")

    lateinit var clickContent: () -> Unit
    lateinit var clickHeader: () -> Unit
    lateinit var hover: (hovered: Boolean) -> Unit

    var hoveredElement = false

    var expanded = false
        set(value) {
            field = value
//            content.enabled = value

            texture.textureLocation = if (expanded) up else down

            header.color = Color(42, 102, 189)
        }

    val texture = rectangle {
        size = V3(8.0, 8.0)
        origin = RIGHT
        align = RIGHT
        textureLocation = if (expanded) up else down
        offset = V3(-6.0, 0.0)
        color = Color(255, 255, 255)
    }

    val backElement = +carved {
        carveSize = 2.0
        size = V3(111.5, 19.0)
        color = Color(21, 53, 98, 0.86)
    }

    val content = +flex {
        flexDirection = FlexDirection.DOWN
        flexSpacing = 0.0

        offset = V3(0.0, 19.0)

        contentSelect.forEach {
            +carved {
                carveSize = 2.0
                size = V3(111.5, 19.0)
                color = Color(0, 0, 0, 0.0)

                val text = +text {
                    content = it
                    origin = LEFT
                    align = LEFT
                    offset = V3(6.0, 0.0)
                }

                val selectColor = Color(42, 102, 189)
                val hoverColor = Color(74, 140, 236)
                val defaultColor = Color(0, 0, 0, 0.0)

                var hoverElement = false

                beforeTransform {
                    color = if (headerText.content == text.content) selectColor else {
                        if (hoverElement) hoverColor else defaultColor
                    }
                }

                onHover {
                    hoverElement = hovered

                    if (headerText.content != text.content) {
                        color = if (hovered) hoverColor else defaultColor
                    }
//
//                    if (headerText.content == text.content) {
//                        color = Color(42, 102, 189)
//                    } else color = if (hovered) Color(74, 140, 236) else Color(0, 0, 0, 0.0)
                }

                onMouseDown {
                    if (headerText.content == text.content || !expanded) return@onMouseDown

                    animate(0.2, Easings.CIRC_OUT) {
                        this@flex.children.forEach { it.color = Color(0, 0, 0, 0.0) }
                        color = Color(39, 84, 149)
                    }

                    headerText.content = text.content
                    expanded = false

                    clickContent()
                }
            }
        }
    }

    val header = +carved {
        carveSize = 2.0
        size = V3(111.5, 19.0)
        color = Color(42, 102, 189)

        +texture

        onHover {
            if (expanded) return@onHover

            animate(0.2, Easings.CIRC_OUT) {
                color = if (hovered) Color(74, 140, 236) else Color(42, 102, 189)
            }
        }

        onMouseDown {
            expanded = !expanded

            clickHeader()

            animate(0.2, Easings.CIRC_OUT) {
                color = if (expanded) Color(39, 84, 149) else Color(74, 140, 236)
            }
        }
//        onLeftClick {
//            expanded = !expanded
//
//            animate(0.228, Easings.CIRC_OUT) {
//                color = if (expanded) Color(39, 84, 149) else Color(74, 140, 236)
//            }
//        }
    }

    val headerText = header + text {
        content = contentHeader
        align = LEFT
        origin = LEFT
        offset = V3(6.0, 0.0)
    }

    init {
        size = header.size
//        size.y = 200.0

//        color = Color(42, 102, 189, 0.101)
        color = Color(255, 255, 255)
        colorMask = false

        mask = true
        expanded = false

//        color = Color(42, 102, 189, 0.101)

        onHover {
            hoveredElement = hovered
            hover(hovered)
        }

        beforeTransform {
            val sizeY = header.size.y + if (expanded && content.children.isNotEmpty()) content.size.y else 0.0

            if (size.y != sizeY) {
                animate(0.2, Easings.CIRC_OUT) {
                    backElement.size.y = sizeY
                    size.y = sizeY

                    offset.z = if (expanded) 50.0 else 0.0
                }
            }
        }
    }
}