import dev.xdark.clientapi.event.lifecycle.GameLoop
import dev.xdark.clientapi.event.render.ScaleChange
import dev.xdark.clientapi.event.window.WindowResize
import dev.xdark.clientapi.opengl.GlStateManager
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.Display
import ru.cristalix.clientapi.JavaMod.loadTextureFromJar
import ru.cristalix.uiengine.UIEngine
import ru.cristalix.uiengine.UIEngine.clientApi
import ru.cristalix.uiengine.UIEngine.overlayContext
import ru.cristalix.uiengine.element.CarvedRectangle
import ru.cristalix.uiengine.element.ContextGui
import ru.cristalix.uiengine.element.RectangleElement
import ru.cristalix.uiengine.eventloop.animate
import ru.cristalix.uiengine.onMouseDown
import ru.cristalix.uiengine.utility.*
import kotlin.math.abs
import kotlin.math.sign

val searchIcon = loadTextureFromJar(clientApi, "compass", "search", "search.png")
val starIcon = loadTextureFromJar(clientApi, "compass", "star", "star.png")
val loading = loadTextureFromJar(clientApi, "compass", "loading", "loading.png")
val categories = listOf(
    Category("Игры", "Загрузка") {
        compass.games
            .sortedBy { -it.online }
            .sortedBy { it.starred.not() }
            .sortedBy { it.realmType == "HUB" }
    },
    Category(
        "Избранные",
        "Упс!\n\nУ вас нет избранных\nрежимов, выберите их в играх."
    ) { compass.games.filter { it.starred }.sortedBy { -it.online } },
    Category(
        "Хабы",
        "Ой...\n\nА где все хабы?"
    ) { compass.games.find { it.realmType == "HUB" }?.subGames?.sortedBy { -it.online } ?: listOf() }
)
val headerHeight = 22.0
val headerPadding = 5.0
val columns = 6
val fielRelativeHeight = 198.0 + 16

class CompassGui(val data: Compass, category: String = "Игры") : ContextGui() {
    var updateTagsState = true
    var activeCategory = categories.first { it.hint.content == category }
    var games = listOf<CompassNode>()
    var openned = true
    val headerSizeX = overlayContext.size.x * 0.55
    val emptyCategoryText = +text {
        align = CENTER
        origin = CENTER
        scale = V3(1.5, 1.5, 1.5)
        color = WHITE
        shadow = true
    }
    lateinit var scrollElement: RectangleElement
    var draggingStart = 0.0
    val scrollContainer = +carved {
        size = V3(3.0, overlayContext.size.y - 4 * headerPadding)
        color = Color(21, 53, 98, 0.62)
        align = RIGHT
        origin = RIGHT
        offset.x = -headerPadding

        scrollElement = +rectangle {
            align = TOP
            origin = TOP
            size = V3(3.0 * 10, 140.0)

            +carved {
                align = CENTER
                origin = CENTER
                size = V3(3.0, 140.0)
                color = Color(42, 102, 189, 1.0)
            }

            onMouseDown {
                val scale = clientApi.resolution().scaleFactor
                draggingStart = (Mouse.getY() / scale).toDouble()
            }
        }
    }

    var lastElementHeight = fielRelativeHeight * 0.5

    var animation = false

    fun scrollable() = games.size > 3 * columns

    fun scrollHeight() = lastElementHeight * (games.size / columns - 2)

    var scroll = 0.0
        set(value) {
            if (animation) return
            animation = true
            scrollElement.animate(0.05, Easings.QUINT_BOTH) {
                container.offset.y = 4 * headerPadding + headerHeight - value
                scrollElement.offset.y =
                    value / scrollHeight() * (overlayContext.size.y - 4 * headerPadding - scrollElement.size.y)
            }
            UIEngine.schedule(0.06) { animation = false }
            field = value
        }

    private val hoverTags = flex {
        overflowWrap = true
        flexSpacing = 2.0
        offset.x += 4
    }

    private val hoverTextScale = 0.5 + 0.25 + 0.125
    private val hoverTitle = text {
        shadow = true
        lineHeight += 5
        scale = V3(0.75, 0.75, 0.75)
        color = WHITE
        offset = V3(4.0, 4.0)
    }
    private val hoverText = text {
        shadow = true
        lineHeight += 2
        scale = V3(0.75, 0.75, 0.75)
        color = WHITE
        offset = V3(4.0, 4.0 + hoverTitle.lineHeight)
    }
    val hoverCenter = carved {
        color = Color(54, 54, 54, 1.0)
        offset = V3(1.0, 1.0)
        +hoverTitle
        +hoverText
        +hoverTags
    }
    val hoverContainer = carved {
        color = Color(75, 75, 75, 0.38)
        enabled = false
        +hoverCenter

        beforeRender { GlStateManager.disableDepth() }
        afterRender { GlStateManager.enableDepth() }
    }

    fun getTextScale(): Double {
        val maxString = games.maxByOrNull {
            (it.compassGame.title?.split("\n")?.maxByOrNull { it.length } ?: "").length
        }?.compassGame?.title
            ?.split("\n")
            ?.maxByOrNull { it.length } ?: ""
        val scaled = clientApi.fontRenderer().getStringWidth("$maxString  ")

        return if (scaled * 0.75 >= overlayContext.size.x * 0.55 * 0.16666) 0.5 else 0.75
    }

    fun redraw(additionalSort: (CompassGame) -> Int = { -2000 }) {
        data.games.filter { playerData?.favorite?.contains(it.depend ?: it.realmType) ?: false }
            .forEach { it.starred = true }
        container.offset.y = 4 * headerPadding + headerHeight
        scroll = 0.0
        scrollContainer.size.y = overlayContext.size.y - 4 * headerPadding
        hoverContainer.enabled = false

        val loadedGames = games
        loadedGames.forEach { container.removeChild(it.game) }
        var pregames = activeCategory.games.invoke()
        if (search.contentText.content.replace("|", "").isNotEmpty())
            pregames = pregames.sortedBy { additionalSort.invoke(it) }.take(6 + (Math.random() * 5).toInt())
        games = pregames.map { CompassNode(it) }.onEach {
            val node = it.apply {
                val x = header.size.x / columns - padding * (columns.toFloat() - 1) / columns.toFloat() - 0.1
                val y = fielRelativeHeight / 160.0 * x
                val icon = y * 148.0 / fielRelativeHeight
                game.size = V3(x, y)
                lastElementHeight = y * 1.5
                hint.size = game.size
                image.size.x = icon
                image.size.y = icon

                image.textureLocation =
                    if (it.compassGame.parent != null && image.textureLocation == loading) it.compassGame.parent?.image
                    else it.compassGame.image

                contentBox.size.y = y - icon
                contentBox.size.x = x
                contentBox.offset.y = icon
            }
            node.game.onHover {
                val game = it.compassGame
                if (hovered && game.description?.isNotEmpty() == true) {
                    val desc = game.description!!

                    hoverContainer.size.x =
                        clientApi.fontRenderer()
                            .getStringWidth(desc.maxByOrNull { it.length } ?: "")
                            .toDouble() * hoverTextScale + 4
                    val padding = 16.0
                    hoverContainer.size.y =
                        hoverText.lineHeight * desc.count() * hoverTextScale + 2.0 + hoverTitle.lineHeight * hoverTextScale + padding
                    hoverTags.offset.y = hoverContainer.size.y - padding
                    hoverCenter.size.x = hoverContainer.size.x - 2
                    hoverCenter.size.y = hoverContainer.size.y - 2

                    if (!hoverContainer.enabled && !header.hovered) {
                        hoverTitle.content = game.title?.replace("\n", " ") + " §b" + game.online
                        hoverText.content = desc.joinToString("\n").replace("&", "§")
                        if (hoverText.content.endsWith("\n"))
                            hoverText.content = hoverText.content.dropLast(2)

                        hoverTags.children.clear()
                        game.createTags()?.let {
                            it.forEach { tag ->
                                hoverTags.addChild(tag)
                            }
                        }

                        hoverTags.size = V3(hoverCenter.size.x, hoverCenter.size.y / 4)
                        hoverTags.update()

                        hoverContainer.enabled = true
                    }
                } else {
                    hoverContainer.enabled = false
                }
            }
            container + node.game
        }

        val sized = getTextScale()
        val scaled = V3(sized, sized, sized)

        games.forEach {
            it.online.scale = scaled
            it.content.scale = scaled
        }

        scrollContainer.enabled = scrollable()
        emptyCategoryText.content = if (games.isEmpty()) activeCategory.empty else ""
    }

    val search = input(this) {
        align = LEFT
        origin = LEFT
        offset.x += headerPadding
        color = Color(42, 102, 189, 1.0)
        size = V3(115.0, headerHeight - 2 * headerPadding, 1.0)
        +rectangle {
            align = RIGHT
            origin = RIGHT
            offset.x -= headerPadding
            size = V3(headerHeight - 3 * headerPadding, headerHeight - 3 * headerPadding)
            color = WHITE
            textureLocation = searchIcon
        }
    }.apply {
        container.origin = LEFT
        container.align = LEFT
        container.offset.x += headerPadding
        container.scale = V3(0.75, 0.75, 0.75)
        hintText.content = "Поиск"
        hintText.origin = LEFT
        hintText.align = LEFT
        hintText.offset.x += headerPadding
        hintText.scale = V3(0.75, 0.75, 0.75)
    }

    val categoriesContainer = flex {
        align = RIGHT
        origin = RIGHT
        size = V3(200.0, headerHeight, 1.0)
        offset.x -= headerPadding
        flexSpacing = headerPadding
        categories.forEach { +it.button }
    }

    val padding = 4.0

    var bannerContainer: RectangleElement? = null
    var banner: CarvedRectangle? = null

    val container = +flex {
        align = TOP
        origin = TOP
        offset.y += 4 * headerPadding + headerHeight
        size.x = headerSizeX
        flexSpacing = padding
        overflowWrap = true

        /*if (compass.banners.isEmpty()) {
            val bannerText = text {
                align = LEFT
                origin = LEFT
                color = WHITE
                offset.x += headerPadding * 4
                content = "Новый год!\nСкидка на донат 30%"
                scale = V3(1.25, 1.25)
                shadow = true
            }

            banner = carved {
                carveSize = 2.0
                color = Color(100, 100, 100)
                size.y =
                    (headerSizeX / columns - padding * (columns.toFloat() - 1) / columns.toFloat() - 0.1) / 148.0 * fielRelativeHeight
                size.x = headerSizeX
                +bannerText
            }

            bannerContainer = +rectangle {
                +banner!!
                size = banner!!.size
                size.y += headerPadding
            }
        }*/
    }

    val header = +carved {
        align = TOP
        origin = TOP
        offset.y += 2 * headerPadding
        color = Color(21, 53, 98, 0.62)
        size = V3(headerSizeX, headerHeight, 1.0)
        carveSize = 2.0
        +search
        +categoriesContainer
        beforeRender { GlStateManager.disableDepth() }
        afterRender { GlStateManager.enableDepth() }
        onHover { if (hovered) hoverContainer.enabled = false }
    }

    fun resize() {
        header.size.x = overlayContext.size.x * 0.6
        container.size.x = header.size.x
        if (compass.banners.isNotEmpty()) {
            banner!!.size.x = header.size.x
            banner!!.size.y =
                (header.size.x / columns - padding * (columns.toFloat() - 1) / columns.toFloat() - 0.1) / 148.0 * fielRelativeHeight
            bannerContainer!!.size = banner!!.size
            bannerContainer!!.size.y += headerPadding
        }
        redraw()
    }

    init {
        color = Color(0, 0, 0, 0.86)

        +hoverContainer
        redraw()
        onKeyTyped { _, code ->
            if (code == Keyboard.KEY_ESCAPE)
                openned = false
            else if (code == Keyboard.KEY_RETURN)
                mod.join(games.firstOrNull()?.compassGame ?: return@onKeyTyped)
        }

        mod.registerHandler<GameLoop> {
            if (openned) {
                val scale = clientApi.resolution().scaleFactor
                val mouseY = Mouse.getY()
                val mouseX = Mouse.getX()

                val out = mouseX / scale + hoverContainer.size.x + 10 > overlayContext.size.x

                hoverContainer.offset.x = mouseX / scale + if (out) -(6.0 + hoverContainer.size.x) else 6.0
                hoverContainer.offset.y = (Display.getHeight() - mouseY) / scale - 6.0

                if (draggingStart != 0.0 && !Mouse.isButtonDown(0))
                    draggingStart = 0.0
                if (scrollable() && !animation) {
                    val nowMouse = (mouseY / scale).toDouble()
                    val dy =
                        if (abs(draggingStart - nowMouse) < 1 || draggingStart == 0.0) 0.0 else nowMouse - draggingStart

                    val wheel = Mouse.getDWheel()

                    if (dy == 0.0 && wheel == 0) return@registerHandler

                    val move = sign(-wheel.toDouble()).toInt() * 75.0 - dy * 1.8
                    scroll = maxOf(0.0, minOf(scroll + move, scrollHeight()))
                    if (dy != 0.0) draggingStart = nowMouse
                }
            }
        }

        mod.registerHandler<WindowResize> { resize() }
        mod.registerHandler<ScaleChange> { resize() }

        val russianSymbols = "йцукенгшщзхъфывапролджэячсмитьбю"
        val englishSymbols = "qwertyuiopasdfghjklzxcvbnm[];',."
        val replacement = "qйwцeуrкtеyнuгiшoщpз[х]ъsыdвfаgпhрjоkлlд;ж'эzяxчcсvмbиnтmь,б.ю"
        val replace = mapOf(
            'ж' to 'j',
            'е' to 'e',
            'e' to 'и',
            't' to 'т',
            'д' to 'd',
            'с' to 's',
            's' to 'с',
            'p' to 'п',
            'n' to 'н',
            'b' to 'б',
            'o' to 'о',
            'c' to 'с',
            'к' to 'k',
            'и' to 'e',
            'а' to 'y',
            'в' to 'w',
            'г' to 'g',
            'v' to 'в',
            'б' to 'b',
            'п' to 'p',
            'р' to 'p',
            'l' to 'л',
            'л' to 'l',
            'k' to 'к'
        )

        var symbolsCount = search.contentText.content.length
        beforeRender {
            if (updateTagsState) {
                updateTagsState = false
                categories.forEach { category ->
                    category.button.color = if (activeCategory == category) Color(21, 53, 98, 1.0)
                    else Color(42, 102, 189, 1.0)
                }
            }
            val searchText = search.contentText.content
                .replace("|", "")
                .replace(" ", "")
                .lowercase()
                .replace("дж", "j")
                .replace("eve", "иве")
                .replace("при", "pri")

            if (search.typing && symbolsCount != searchText.length) {
                symbolsCount = searchText.length

                val russian = search.contentText.content
                    .filter { englishSymbols.contains(it) }
                    .map { replacement[replacement.indexOf(it) + 1] }
                val english = search.contentText.content
                    .filter { russianSymbols.contains(it) }
                    .map { replacement[maxOf(0, replacement.indexOf(it))] }

                redraw { game ->
                    val title = game.title?.lowercase() ?: ""
                    var sum =
                        if (title.contains(russian.toString()) ||
                            title.contains(english.toString()) ||
                            title.contains(search.contentText.content)
                        ) 7500 * (search.contentText.content.length - 1) else 0
                    search.contentText.content.lowercase().forEachIndexed { index, c ->
                        if (index >= title.length)
                            return@forEachIndexed
                        val dx = minOf(abs(c - title[index]), abs((replace[c] ?: c) - title[index]))
                        val rdx = if (index >= russian.size) 40 else abs(russian[index] - title[index]) / 5
                        val edx = if (index >= english.size) 40 else abs(english[index] - title[index]) / 5
                        sum += (search.contentText.content.count() - index) * 3 *
                                (if (dx == 0 || rdx == 0 || edx == 0) 600 else maxOf(
                                    0,
                                    300 - dx / 15 - minOf(rdx, edx)
                                ))

                    }
                    -sum
                }
            }
        }
    }
}
