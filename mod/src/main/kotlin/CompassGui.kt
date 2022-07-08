import dev.xdark.clientapi.event.lifecycle.GameLoop
import dev.xdark.clientapi.event.render.ScaleChange
import dev.xdark.clientapi.event.window.WindowResize
import dev.xdark.clientapi.opengl.GlStateManager
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import ru.cristalix.clientapi.JavaMod.loadTextureFromJar
import ru.cristalix.uiengine.UIEngine
import ru.cristalix.uiengine.UIEngine.clientApi
import ru.cristalix.uiengine.UIEngine.overlayContext
import ru.cristalix.uiengine.element.CarvedRectangle
import ru.cristalix.uiengine.element.ContextGui
import ru.cristalix.uiengine.element.RectangleElement
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

class CompassGui(compass: Compass, category: String = "Игры") : ContextGui() {
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
    lateinit var scrollElement: CarvedRectangle
    val scrollContainer = +carved {
        size = V3(3.0, overlayContext.size.y - 4 * headerPadding)
        color = Color(21, 53, 98, 0.62)
        align = RIGHT
        origin = RIGHT
        offset.x = -headerPadding
        scrollElement = +carved {
            size = V3(3.0, 140.0)
            color = Color(42, 102, 189, 1.0)
            align = TOP
            origin = TOP
        }
    }
    fun scrollable() = games.size > 3 * columns
    var scroll = 0.0
        set(value) {
            scrollElement.offset.y = value / (70 * games.size / columns) * (overlayContext.size.y - headerPadding - scrollElement.size.y)
            field = value
        }

    fun getTextScale(): Double {
        val maxString = games.maxOfOrNull { it.compassGame.title?.split("\n")?.maxByOrNull { it.count() } ?: "" } ?: ""
        val scaled = clientApi.fontRenderer().getStringWidth(maxString)
        return if (scaled * 0.75 > overlayContext.size.x * 0.55 * 0.16) 0.5 else 0.75
    }

    fun redraw(additionalSort: (CompassGame) -> Int = { 0 }) {
        compass.games.filter { playerData?.favorite?.contains(it.realmType) ?: false }
            .forEach { it.starred = true }

        container.offset.y = 4 * headerPadding + headerHeight
        scroll = 0.0
        scrollContainer.size.y = overlayContext.size.y - 4 * headerPadding

        val loadedGames = games
        loadedGames.forEach { container.removeChild(it.game) }

        var pregames = activeCategory.games.invoke()

        if (search.contentText.content.isNotEmpty())
            pregames = pregames.sortedBy { additionalSort.invoke(it) }

        games = pregames.map { CompassNode(it) }.onEach {
            val node = it.apply {
                val x = header.size.x / columns - padding * (columns.toFloat() - 1) / columns.toFloat() - 0.1
                val y = fielRelativeHeight / 160.0 * x
                val icon = y * 148.0 / fielRelativeHeight
                game.size = V3(x, y)
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
            container + node.game
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

        if (compass.banners.isNotEmpty()) {
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
        }
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
    }

    init {
        color = Color(0, 0, 0, 0.86)

        redraw()
        onKeyTyped { _, code ->
            if (code == Keyboard.KEY_ESCAPE)
                openned = false
            else if (code == Keyboard.KEY_RETURN)
                mod.join(games.firstOrNull()?.compassGame?.realmType ?: "HUB")
        }

        mod.registerHandler<GameLoop> {
            if (openned && scrollable()) {
                val wheel = Mouse.getDWheel()
                val move = sign(-wheel.toDouble()).toInt() * 30
                if (wheel != 0 && scroll + move < 70 * games.size / columns && scroll + move >= 0) {
                    scroll += move
                    container.offset.y -= move
                } else if (scroll < 0) {
                    scroll = 0.0
                    container.offset.y = 4 * headerPadding + headerHeight
                }
            }
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

        mod.registerHandler<WindowResize> { resize() }
        mod.registerHandler<ScaleChange> { resize() }

        var symbolsCount = search.contentText.content.length
        beforeRender {
            if (updateTagsState) {
                updateTagsState = false
                categories.forEach { category ->
                    category.button.color = if (activeCategory == category) Color(21, 53, 98, 1.0)
                    else Color(42, 102, 189, 1.0)
                }
            }
            val searchText = search.contentText.content.replace("|", "").replace(" ", "").lowercase()

            if (search.typing && symbolsCount != searchText.length) {
                symbolsCount = searchText.length
                val russian = search.contentText.content
                    .replace('q', 'й').replace('w', 'ц').replace('e', 'у')
                    .replace('r', 'к').replace('t', 'е').replace('y', 'н')
                    .replace('u', 'г').replace('i', 'ш').replace('o', 'щ')
                    .replace('p', 'з').replace('[', 'х').replace(']', 'ъ')
                    .replace('a', 'ф').replace('s', 'ы').replace('d', 'в')
                    .replace('f', 'а').replace('g', 'п').replace('h', 'р')
                    .replace('j', 'о').replace('k', 'л').replace('l', 'д')
                    .replace(';', 'ж').replace('\'', 'э')
                    .replace('z', 'я').replace('x', 'ч').replace('c', 'с')
                    .replace('v', 'м').replace('b', 'и').replace('n', 'т')
                    .replace('m', 'ь').replace(',', 'б').replace('.', 'ю')

                redraw { game ->
                    val title = game.title?.lowercase() ?: ""
                    var sum = 0
                    search.contentText.content.lowercase().forEachIndexed { index, c ->
                        if (index >= title.length)
                            return@forEachIndexed
                        val dx = abs(c - title[index])
                        val rdx = abs(russian[index] - title[index])
                        sum += (search.contentText.content.count() - index) *
                                (if (dx == 0 || rdx == 0) 100 else maxOf(0, 100 - dx - rdx / 20))

                    }
                    -sum
                }
            }
        }
    }
}
