import dev.xdark.clientapi.event.lifecycle.GameLoop
import dev.xdark.clientapi.event.render.ScaleChange
import dev.xdark.clientapi.event.window.WindowResize
import dev.xdark.clientapi.opengl.GlStateManager
import dev.xdark.clientapi.render.ScaledResolution
import dev.xdark.clientapi.resource.ResourceLocation
import element.DropElement
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.Display
import ru.cristalix.clientapi.JavaMod.loadTextureFromJar
import ru.cristalix.uiengine.UIEngine.clientApi
import ru.cristalix.uiengine.element.CarvedRectangle
import ru.cristalix.uiengine.element.ContextGui
import ru.cristalix.uiengine.element.RectangleElement
import ru.cristalix.uiengine.eventloop.AnimationContext
import ru.cristalix.uiengine.eventloop.animate
import ru.cristalix.uiengine.onMouseDown
import ru.cristalix.uiengine.onMouseUp
import ru.cristalix.uiengine.utility.*
import java.time.Instant
import java.time.OffsetDateTime
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.streams.toList

val searchIcon: ResourceLocation = loadTextureFromJar(clientApi, "compass", "search", "search.png")
val starIcon: ResourceLocation = loadTextureFromJar(clientApi, "compass", "star", "star.png")
val loading: ResourceLocation = loadTextureFromJar(clientApi, "compass", "loading", "loading.png")

val favoriteCategory = Category(
    "Избранные",
    "Упс!\n\nУ вас нет избранных\nрежимов, выберите их в играх."
) {
    compass.games
        .filter { it.starred }
        .sortedBy { -it.online }
}

val allGameCategory = Category(
    "Все игры",
    "Ой...\n\nА где все игры?"
) {
    compass.games
        .sortedBy { it.dynamic }
        .sortedBy { -it.online }
        .sortedBy { it.starred.not() }
        .sortedBy {
            if (it.newActive && it.newDateBefore.isNotEmpty()) {
                if (OffsetDateTime.parse(it.newDateBefore).toInstant() <= Instant.now()) 2 else 0
            } else 0
        }
        .sortedBy { it.realmType == "HUB" }
}

val hubCategory = Category(
    "Хабы",
    "Ой...\n\nА где все хабы?"
) {
    compass.games
        .find { it.realmType == "HUB" }
        ?.subGames
        ?.sortedBy { it.title?.drop(5)?.toInt() } ?: listOf()
}

val testCategory = Category(
    "Тестовые",
    "Ой...\n\nА где все Тестовые сервера?"
) {
    compass.games
        .find { it.realmType == "TEST" }
        ?.subGames ?: listOf()
}

val headerHeight = 33.0
val headerPadding = 7.0

var locationBanenr: ResourceLocation? = null

class CompassGui(val data: Compass) : ContextGui() {
    var updateTagsState = true
    var activeCategory = allGameCategory

    var rootElement: RectangleElement? = null
    var hoverContainer: CarvedRectangle? = null

    var games = listOf<CompassNode>()
    var openned = true

    var totalOnline = 0
    var cdClick = 0L

    val categories = mutableMapOf(
        "ALL_GAMES" to allGameCategory,
        "HUBS" to hubCategory,
        "TEST_SERVERS" to testCategory,
    )

    val emptyCategoryText = +text {
        align = CENTER
        origin = CENTER
        scale = V3(1.5, 1.5, 1.5)
        color = WHITE
    }

    var scrollElement: CarvedRectangle

    var scrollHover = false
    var scrollActiveHover = false

    val scrollContainer = +carved {
        carveSize = 2.0
        size = V3(6.0, 510.0)
        align = RIGHT
        origin = RIGHT
        offset.x = -10.0
        color = Color(21, 53, 98, 0.62)

        scrollElement = +carved {
            carveSize = 2.0
            size = V3(6.0, 188.5)
            align = TOP
            origin = TOP
            color = Color(42, 102, 189, 1.0)

            onHover {
                scrollHover = hovered
            }
        }

        beforeTransform {
            if (openned) {
                val gameSizes = games.size
                val gameColum = 6.0

//                var bannerActive = false

//                val banners = compass.banners
//
//                if (banners != null) {
//                    val banner = banners[0]
//
//                    if (banner.afterTime.isNotEmpty() && banner.beforeTime.isNotEmpty()) {
//                        val nowSeconds = Instant.now().epochSecond
//                        val afterSeconds = OffsetDateTime.parse(banner.afterTime).toInstant().epochSecond
//                        val beforeSeconds = OffsetDateTime.parse(banner.beforeTime).toInstant().epochSecond
//
//                        if ((afterSeconds <= nowSeconds) && (nowSeconds <= beforeSeconds)) bannerActive = true
//                    }
//                }

                val banners = banners()

                val line = ceil(gameSizes / gameColum)
                val elementsSizeY = ((line * 111) + ((line - 1) * 7.0)) + (if (banners.isNotEmpty()) 118.0 else 0.0)
//                val elementsSizeY = ((line * 111) + ((line - 1) * 7.0)) + (if (banners) 118.0 else 0.0)

                val maxSizeMask = size.y

                if (elementsSizeY > maxSizeMask) {
                    val inWhat = elementsSizeY / maxSizeMask
                    val sizeY = maxSizeMask / inWhat

                    if (sizeY >= 20.0) scrollElement.size.y = sizeY
                    else scrollElement.size.y = 20.0
                } else scrollElement.size.y = maxSizeMask
            }
        }

        onMouseDown {
            if (openned) {
                dropMenu!!.expanded = false
                dropMenu!!.content.enabled = false
            }
        }
    }

    private val hoverTitle = text {
        lineHeight += 5

        align = TOP_LEFT
        origin = TOP_LEFT

        offset = V3(4.0, 4.0)
        color = WHITE
    }

    private val hoverText = text {
        lineHeight += 3

        align = TOP_LEFT
        origin = TOP_LEFT

        color = WHITE
        offset = V3(4.0, 4.0 + hoverTitle.lineHeight)
    }

    private val hoverTags = rectangle {
        align = TOP_LEFT
        origin = TOP_LEFT

        offset = V3(4.0, 0.0)
    }

    var hoverCenter = carved {
        carveSize = 2.0
        align = CENTER
        origin = CENTER
        color = Color(54, 54, 54)

        +hoverTitle
        +hoverText
        +hoverTags
    }

    var hoveredDropMenu = false
    var hoveredHeaderMenu = false

    fun redraw(updateBanner: Boolean = true, additionalSort: (CompassGame) -> Int = { -2000 }) {
        val searchEmpty = search.contentText.content.replace("|", "").lowercase().isEmpty()

        header.removeChild(categoriesContainer)
        header.addChild(categoriesContainer.apply {
            if (dropMenu != null) removeChild(dropMenu!!)

            dropMenu = drop(activeCategory.title, categories.values.stream().map {
                it.title
            }.toList()) {
                origin = TOP_RIGHT
                align = TOP_RIGHT
            }

            addChild(dropMenu!!.apply {
                expanded = false
                content.enabled = false

                clickContent = {
                    val name = headerText.content

                    categories.values.forEach {
                        if (it.title == name) {
                            compassGui?.activeCategory = it
                            compassGui?.updateTagsState = true
                            compassGui?.redraw()

                            hoverContainer?.enabled = false

                            cdClick = (System.currentTimeMillis() / 1000) + 2L

                            expanded = false
                            content.enabled = false

                            hoveredDropMenu = false
                            hoveredHeaderMenu = false
                        }
                    }
                }

                clickHeader = {
                    content.enabled = expanded
                }

                hover = {
                    hoveredDropMenu = it
                    hoverContainer?.enabled = false
                }
            })
        })

        data.games
            .filter { playerData?.favorite?.contains(it.depend ?: it.realmType) ?: false }
            .forEach { it.starred = true }

        val loadedGames = games
        loadedGames.forEach { container.removeChild(it.game) }

        var pregames = activeCategory.games.invoke()

        val textSearch = search.contentText.content
            .replace("|", "")
            .replace("\n", "")
            .replace(" ", "")
            .lowercase()
        if (textSearch.isNotEmpty()) {

            val replaceEn = mapOf(
                'й' to 'q',
                'ц' to 'w',
                'у' to 'e',
                'к' to 'r',
                'е' to 't',
                'н' to 'y',
                'г' to 'u',
                'ш' to 'i',
                'щ' to 'o',
                'з' to 'p',
                'х' to '[',
                'ъ' to ']',
                'ф' to 'a',
                'ы' to 's',
                'в' to 'd',
                'а' to 'f',
                'п' to 'g',
                'р' to 'h',
                'о' to 'j',
                'л' to 'k',
                'д' to 'l',
                'ж' to ';',
                'э' to '\'',
                'я' to 'z',
                'ч' to 'x',
                'с' to 'c',
                'м' to 'v',
                'и' to 'b',
                'т' to 'n',
                'ь' to 'm',
                'б' to ',',
                'ю' to '.',
                'ё' to '`'
            )

            val replaceRu = mapOf(
                'q' to 'й',
                'w' to 'ц',
                'e' to 'у',
                'r' to 'к',
                't' to 'е',
                'y' to 'н',
                'u' to 'г',
                'i' to 'ш',
                'o' to 'щ',
                'p' to 'з',
                '[' to 'х',
                ']' to 'ъ',
                'a' to 'ф',
                's' to 'ы',
                'd' to 'в',
                'f' to 'а',
                'g' to 'п',
                'h' to 'р',
                'j' to 'о',
                'k' to 'л',
                'l' to 'д',
                ';' to 'ж',
                '\'' to 'э',
                'z' to 'я',
                'x' to 'ч',
                'c' to 'с',
                'v' to 'м',
                'b' to 'и',
                'n' to 'т',
                'm' to 'ь',
                ',' to 'б',
                '.' to 'ю',
                '`' to 'ё'
            )

            var english = ""
            var russia = ""

            textSearch.forEach {
                if (replaceEn.keys.contains(it)) {
                    english += replaceEn.getOrDefault(it, it)
                }
                if (replaceRu.keys.contains(it)) {
                    russia += replaceRu.getOrDefault(it, it)
                }
            }

            pregames = pregames.filter { cg ->
                val title = cg.title!!
                    .replace("\n", "")
                    .replace(" ", "")
                    .lowercase()

                title.contains(textSearch)
                        || (english.isNotEmpty() && title.contains(english))
                        || (russia.isNotEmpty() && title.contains(russia))
                        || cg.tags!!.any { it.replace("\n", "").replace(" ", "").lowercase().contains(textSearch) }
                        || cg.keywords!!.any { it.replace("\n", "").replace(" ", "").lowercase().contains(textSearch) }
            }
        }

//            pregames = pregames.sortedBy { additionalSort.invoke(it) }.take(6 + (Math.random() * 5).toInt())

        container.removeChild(bannerContainer!!)

        val banners = banners()

        if (banners.isNotEmpty() && activeCategory == allGameCategory && updateBanner && searchEmpty) {
            val maxBanner = banners.size

            bannerContainer = rectangle {
                size = V3(515.0, 103.0)

                if (maxBanner > 1) {
//                    color = Color(0, 0, 0, 0.101)
                    color = Color(255, 255, 255)
                    colorMask = false
                    mask = true
                }

                val flexInfo = +flex {

                    flexDirection = FlexDirection.RIGHT
                    flexSpacing = 0.0

                    align = LEFT
                    origin = LEFT

                    banners.forEachIndexed { it, banner ->
                        val bannerText = text {
                            content = banner.text.joinToString("\n")
                            align = TOP_LEFT
                            origin = TOP_LEFT
                            offset = V3(17.5, 20.5)
                            scale = V3(2.0, 2.0)
                            color = Color(255, 255, 255)
                        }

                        val button = rectangle {
                            val bannerButton = banner.button!!

                            val textButton = bannerButton.displayName ?: ""

                            val sizeButtonX = clientApi.fontRenderer().getStringWidth(textButton) + 14.0
                            val sizeButtonY = 19.0

                            size = V3(sizeButtonX, sizeButtonY)
                            align = BOTTOM_LEFT
                            origin = BOTTOM_LEFT
                            offset = V3(17.5, -20.0)

                            +carved {
                                carveSize = 2.0
                                size = V3(sizeButtonX, sizeButtonY)
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

                                onMouseDown {
                                    hoverContainer?.enabled = false

                                    val type = bannerButton.type
                                    val value = bannerButton.value

                                    if (type.equals("command")) {
                                        openned = false
                                        close()

                                        clientApi.chat().sendChatMessage(value)
                                    } else if (type.equals("url")) {
                                        clientApi.minecraft().openUrl(value)
                                    }
                                }
                            }

                            +text {
                                content = textButton
                                align = CENTER
                                origin = CENTER
                                color = Color(255, 255, 255)
                            }
                        }

                        val textEnd = text {
                            align = BOTTOM_LEFT
                            origin = BOTTOM_LEFT
                            offset = V3(17.5 + button.size.x + 9.0, -25.5)

                            beforeTransform {
                                if (banner.beforeTime.isNotEmpty()) {
                                    val current = OffsetDateTime.parse(banner.beforeTime).toInstant().epochSecond
                                    val now = Instant.now().epochSecond

                                    val endTime = current - now

                                    if (endTime <= 0) {
                                        val endContent = banner.endedText
                                        if (content != endContent) content = endContent
                                    } else {
                                        val text =
                                            banner.timeText.replace("%time%", StringUtils.getCompleteTime(endTime))
                                        if (content != text) content = text
                                    }
                                }
                            }
                        }

                        +rectangle {
                            size = V3(515.0, 103.0)

                            if (updateBanner) {
                                color = Color(21, 53, 98, 0.62)

                                load(banner.texture ?: "").thenAccept { location ->
                                    run {
                                        locationBanenr = location
                                        textureLocation = location
                                        color = Color(255, 255, 255)
                                    }
                                }
                            } else {
                                color = if (locationBanenr == null) Color(21, 53, 98, 0.62) else Color(
                                    255,
                                    255,
                                    255
                                )
                                textureLocation = locationBanenr
                            }

                            +bannerText
                            +button
                            +textEnd
                        }
                    }
                }

                flexInfo.offset.x = -(selectBanner * 515.0)

                if (maxBanner > 1) {
                    +flex {
                        flexDirection = FlexDirection.RIGHT
                        flexSpacing = 4.0

                        align = BOTTOM
                        origin = BOTTOM

                        offset = V3(0.0, -7.0)

                        banners.forEachIndexed { it, banner ->
                            var timeSlider = banner.timeSlider
                            if (timeSlider < 5) timeSlider = 5

                            +rectangle {
                                size = V3(20.0, 2.0)
                                color = Color(255, 255, 255, 0.28)

                                val timerElement = +rectangle {
                                    size = V3(0.0, 2.0)
                                    align = LEFT
                                    origin = LEFT
                                    color = Color(255, 255, 255, 0.0)
                                }

                                var active = false

                                beforeTransform {
                                    val newActive = selectBanner == it
                                    val colorActive = if (newActive) Color(112, 112, 112, 0.28) else Color(
                                        255,
                                        255,
                                        255,
                                        0.28
                                    )
                                    val colorAlpha = if (newActive) 1.0 else 0.0

                                    if (active != newActive) {
                                        animate(0.4, Easings.CIRC_OUT) {
                                            color = colorActive

                                            timerElement.size.x = 0.0
                                            timerElement.color.alpha = colorAlpha
                                        }

                                        if (newActive) {
                                            animate(timeSlider + 0.8, Easings.SINE_OUT) {
                                                timerElement.size.x = 20.0
                                            }
                                        }

                                        active = newActive
                                    }
                                }

                                onMouseDown {
                                    selectBanner = it
                                    timerOldBanner = (System.currentTimeMillis() / 1000) + timeSlider

                                    animate(0.4, Easings.CIRC_OUT) {
                                        flexInfo.offset.x = -(selectBanner * 515.0)
                                    }
                                }
                            }
                        }
                    }

                    beforeTransform {
                        val current = System.currentTimeMillis() / 1000

                        if (timerOldBanner <= current) {
                            if (openned) {
                                selectBanner++
                                if (selectBanner == maxBanner) selectBanner = 0

                                var timeSlider = banners[selectBanner].timeSlider.toLong()
                                if (timeSlider < 5) timeSlider = 5

                                timerOldBanner = current + timeSlider

                                animate(0.4, Easings.CIRC_OUT) {
                                    flexInfo.offset.x = -(selectBanner * 515.0)
                                }
                            }
                        }
                    }
                }
            }

            container.addChild(bannerContainer!!)
        }

        hoveredDropMenu = false
        hoveredHeaderMenu = false

        games = pregames.map { CompassNode(it) }.onEach { compassNode ->
            val star = compassNode.star
            val gameNode = compassNode.game

            val game = compassNode.compassGame

            var gameNodeHover = false

            gameNode.onHover {
                gameNodeHover = hovered

                if (openned && !hoveredDropMenu && !hoveredHeaderMenu) {
                    animate(if (hovered) 0.2 else 0.3, Easings.CIRC_OUT) {
                        if (hovered) {
                            compassNode.hint.color.alpha = 0.82
                            star.color.alpha = 1.0
                            compassNode.hintText.color.alpha = 1.0

                            compassNode.infoBox.color.alpha = 0.0
                            compassNode.contentInfo.color.alpha = 0.0
                        } else {
                            compassNode.hint.color.alpha = 0.0
                            star.color.alpha = 0.0
                            compassNode.hintText.color.alpha = 0.0

                            compassNode.infoBox.color.alpha = 1.0
                            compassNode.contentInfo.color.alpha = 1.0
                        }
                    }
                }
            }

            var gameNodeHoverEnable = true

            gameNode.beforeTransform {
                if (openned) {
                    if ((hoveredDropMenu || hoveredHeaderMenu) && gameNodeHoverEnable) {
                        animate(0.3, Easings.CIRC_OUT) {
                            compassNode.hint.color.alpha = 0.0
                            star.color.alpha = 0.0
                            compassNode.hintText.color.alpha = 0.0

                            compassNode.infoBox.color.alpha = 1.0
                            compassNode.contentInfo.color.alpha = 1.0
                        }

                        gameNodeHoverEnable = false
                    }

                    if (gameNodeHover && !hoveredDropMenu && !hoveredHeaderMenu && !gameNodeHoverEnable) {
                        animate(0.2, Easings.CIRC_OUT) {
                            compassNode.hint.color.alpha = 0.82
                            star.color.alpha = 1.0
                            compassNode.hintText.color.alpha = 1.0

                            compassNode.infoBox.color.alpha = 0.0
                            compassNode.contentInfo.color.alpha = 0.0
                        }

                        hoverContainer?.enabled = game.description?.isNotEmpty() == true
                        gameNodeHoverEnable = true
                    }

                    if (compassNode.compassGame.infoTag != null) {
                        compassNode.contentInfo.content = compassNode.compassGame.infoTag ?: ""

                        if (compassNode.contentInfo.content.isNotEmpty()) {
                            compassNode.infoBox.size.x =
                                clientApi.fontRenderer().getStringWidth(compassNode.contentInfo.content) + 16.0
                            compassNode.infoBox.enabled = true
                        }
                    }
                }
            }

            star.onHover {
                if (openned && gameNodeHoverEnable) {
                    animate(0.228, Easings.CIRC_OUT) {
                        star.color.alpha = if (hovered) .62 else 1.0
                    }
                }
            }

            val node = compassNode.apply {
                if (game.infoDateBefore.isNotEmpty()) {
                    if (OffsetDateTime.parse(game.infoDateBefore).toInstant() <= Instant.now()) game.infoTag = ""
                }

                compassNode.contentInfo.content = game.infoTag ?: ""

                image.textureLocation =
                    if (compassNode.compassGame.parent != null && image.textureLocation == loading) compassNode.compassGame.parent?.image
                    else compassNode.compassGame.image
            }

            container.offset.y = 30.0 + headerHeight
            scrollElement.offset.y = 0.0

            node.game.onHover {
                val container = hoverContainer ?: return@onHover
                container.enabled = hovered && !hoveredDropMenu && !hoveredHeaderMenu

                if (game.description?.isNotEmpty() == true) {
                    val desc = game.description!!

                    if (hovered) {
                        hoverTitle.content = game.title?.replace("\n", " ") + " ¨4a8cec" + game.online

                        hoverText.content = desc.joinToString("\n").replace("&", "§")
                        if (hoverText.content.endsWith("\n")) hoverText.content = hoverText.content.dropLast(2)

                        hoverTags.children.forEach { i -> removeChild(i) }
                        hoverTags.children.clear()

                        val linesTitle = hoverTitle.content.split("\n")
                        val countLineTitle = linesTitle.size

                        val linesText = hoverText.content.split("\n")
                        val countLineText = linesText.size

                        val sizeTextX =
                            clientApi.fontRenderer().getStringWidth(linesText.maxByOrNull { t -> t.length } ?: "")
                                .toDouble()
                        val sizeY = (hoverTitle.lineHeight * countLineTitle) + (hoverText.lineHeight * countLineText)

                        container.size.x = sizeTextX + 26.0

                        hoverTags.size.x = sizeTextX
                        hoverTags.offset.y = sizeY + 7.0

                        game.createTags()?.let { it.forEach { tag -> hoverTags.addChild(tag) } }

                        var line = 0.0
                        var totalX = 0.0

                        hoverTags.size.y = 12.0

                        hoverTags.children.forEach {
                            val sx = it.size.x

                            if (totalX + sx > sizeTextX) {
                                line++
                                totalX = sx + 3.0

                                it.offset = V3(0.0, 15.0 * line)
                                hoverTags.size.y += 15.0
                            } else {
                                it.offset = V3(totalX, if (line > 0) 15.0 * line else 0.0)
                                totalX += sx + 3.0
                            }
                        }

                        container.size.y = sizeY + hoverTags.size.y + 15.0

                        hoverCenter.size.x = container.size.x - 2.0
                        hoverCenter.size.y = container.size.y - 2.0
                    }
                } else {
                    hoverContainer?.enabled = false
                }
            }

            container + node.game
        }

        cdClick = (System.currentTimeMillis() / 1000) + 5L

        emptyCategoryText.content = if (games.isEmpty()) activeCategory.empty else ""
    }

    val search = input(this) {
        carveSize = 2.0
        size = V3(111.5, 19.0)
        align = LEFT
        origin = LEFT
        offset = V3(headerPadding, 0.0)
        color = Color(42, 102, 189, 1.0)

        allowMultiline = false

        val icon = +rectangle {
            size = V3(8.0, 8.0)
            align = RIGHT
            origin = RIGHT
            offset.x = -6.0
            textureLocation = searchIcon
            color = WHITE
        }

        beforeTransform {
            if (openned) {
                icon.enabled = contentText.content.length <= 15
            }
        }

        onMouseDown {
            if (openned) {
                dropMenu!!.expanded = false
                dropMenu!!.content.enabled = false
            }
        }
    }.apply {
        container.origin = LEFT
        container.align = LEFT
        container.offset.x = 6.0

        hintText.content = "Поиск"
        hintText.origin = LEFT
        hintText.align = LEFT
        hintText.offset.x = 6.0
    }

    private val globalOnline = text {
        align = LEFT
        origin = LEFT
        offset = V3(headerPadding + search.size.x + headerPadding, 0.0)

        beforeTransform {
            content = "Онлайн: $totalOnline"
        }
    }

    var dropMenu: DropElement? = null

    val categoriesContainer = rectangle {
        align = RIGHT
        origin = RIGHT

        offset.x = -7.0

        dropMenu = drop(allGameCategory.title, categories.values.stream().map {
            it.title
        }.toList()) {
            origin = TOP_RIGHT
            align = TOP_RIGHT
        }

        val selectColor = Color(21, 53, 98)
        val defaultColor = Color(42, 102, 189)
        val hoverColor = Color(74, 140, 236)

        +favoriteCategory.button.apply {
            origin = RIGHT
            align = RIGHT

            offset.x = -7.0 - dropMenu!!.size.x

            var hoverElement = false

            beforeTransform {
                if (openned) {
                    color = if (compassGui?.activeCategory == favoriteCategory) selectColor else {
                        if (hoverElement) hoverColor else defaultColor
                    }
                }
            }

            onHover {
                hoverElement = hovered

                if (compassGui?.activeCategory != favoriteCategory) {

                    color = if (hovered) hoverColor else defaultColor
                }
            }

            onMouseDown {
                if (compassGui?.activeCategory == favoriteCategory) {
                    dropMenu!!.headerText.content = allGameCategory.title

                    dropMenu!!.expanded = false
                    dropMenu!!.content.enabled = false

                    dropMenu!!.content.children.first().color = Color(42, 102, 189)

                    compassGui?.activeCategory = allGameCategory
                    compassGui?.updateTagsState = true
                    compassGui?.redraw()
                } else {
                    dropMenu!!.headerText.content = "Фильтр"

                    dropMenu!!.expanded = false
                    dropMenu!!.content.enabled = false

                    dropMenu!!.content.children.forEach { it.color = Color(0, 0, 0, 0.0) }

                    compassGui?.activeCategory = favoriteCategory
                    compassGui?.updateTagsState = true
                    compassGui?.redraw()

                    color = Color(21, 53, 98)
                }
            }
        }

        size = V3(favoriteCategory.button.size.x + 7.0 + dropMenu!!.size.x, 19.0)
    }

    var bannerContainer: RectangleElement? = null

    var selectBanner = 0
    var timerOldBanner = (System.currentTimeMillis() / 1000) + 5L

    private val container = flex {
        size = V3(515.0, 0.0)
        align = TOP
        origin = TOP
        offset = V3(0.0, 30.0 + headerHeight)

        flexSpacing = 7.0
        overflowWrap = true

        onMouseDown {
            if (openned) {
                dropMenu!!.expanded = false
                dropMenu!!.content.enabled = false
            }
        }
    }

    private val header = carved {
        carveSize = 3.0
        size = V3(515.0, headerHeight)
        align = TOP
        origin = TOP
        offset = V3(0.0, 15.0)
        color = Color(21, 53, 98, 0.62)

        +search
        +globalOnline

        onHover { if (hovered) hoverContainer?.enabled = false }

        beforeRender { GlStateManager.disableDepth() }
        afterRender { GlStateManager.enableDepth() }

        onMouseDown {
            if (openned) {
                dropMenu!!.expanded = false
                dropMenu!!.content.enabled = false
            }
        }

        onHover {
            if (openned) {
                hoveredHeaderMenu = hovered
            }
        }
    }

    init {
        onKeyTyped { _, code ->
            if (code == Keyboard.KEY_ESCAPE) {
                hoverContainer?.enabled = false

                dropMenu?.expanded = false
                dropMenu?.content?.enabled = false

                openned = false
            } else if (code == Keyboard.KEY_RETURN) mod.join(games.firstOrNull()?.compassGame ?: return@onKeyTyped)
        }

        color = Color(0, 0, 0, 0.86)

        var lasMouseLoc = 0.0

        onMouseDown {
            if (openned) {
                dropMenu!!.expanded = false
                dropMenu!!.content.enabled = false
            }
        }

        mod.registerHandler<GameLoop> {
            if (openned) {
                val wheel = Mouse.getDWheel()

                val resolution = clientApi.resolution()
                val scale = resolution.scaleFactor

                val mouseY = Mouse.getY().toDouble() / 2.0
                val mouse = Mouse.isButtonDown(0)

                val startOffset = 30.0 + headerHeight
                val sizeY = (resolution.scaledHeight_double * scale / 2.0) - (startOffset)

                val gameSizes = games.size
                val gameColum = 6.0

                val line = ceil(gameSizes / gameColum)
                val maxSize = ((line * 111) + ((line - 1) * 7.0))

//                var bannerActive = false
//
//                val banners = compass.banners
//
//                if (banners != null) {
//                    val banner = banners[0]
//
//                    if (banner.afterTime.isNotEmpty() && banner.beforeTime.isNotEmpty()) {
//                        val nowSeconds = Instant.now().epochSecond
//                        val afterSeconds = OffsetDateTime.parse(banner.afterTime).toInstant().epochSecond
//                        val beforeSeconds = OffsetDateTime.parse(banner.beforeTime).toInstant().epochSecond
//
//                        if ((afterSeconds <= nowSeconds) && (nowSeconds <= beforeSeconds)) bannerActive = true
//                    }
//                }

                val searchEmpty = search.contentText.content.replace("|", "").lowercase().isEmpty()

                val banners = banners()

                val maxOffset = -(maxSize - (startOffset - 15.0)) + (rootElement!!.size.y - (startOffset - 15.0)) - (if (activeCategory == allGameCategory && banners.isNotEmpty() && searchEmpty) 118.0 else 0.0)
//                val maxOffset = -(maxSize - (startOffset - 15.0)) + (rootElement!!.size.y - (startOffset - 15.0)) - (if (activeCategory == allGameCategory && banners.isNotEmpty()) 118.0 else 0.0)
//                val maxOffset = -(maxSize - (startOffset - 15.0)) + (rootElement!!.size.y - (startOffset - 15.0)) - (if (activeCategory == allGameCategory && bannerActive) 118.0 else 0.0)

                if (mouse) {
                    if (scrollHover && !scrollActiveHover) {
                        scrollActiveHover = true
                        lasMouseLoc = mouseY

                        dropMenu!!.expanded = false
                        dropMenu!!.content.enabled = false
                    }

                    if (scrollActiveHover && (scrollContainer.size.y != scrollElement.size.y)) {
                        val max = scrollContainer.size.y - scrollElement.size.y

                        if (mouseY > lasMouseLoc) {
                            val total = scrollElement.offset.y - (mouseY - lasMouseLoc)
                            scrollElement.offset.y = if (total < 0.0) 0.0 else total
                        }

                        if (mouseY < lasMouseLoc) {
                            val total = scrollElement.offset.y + (lasMouseLoc - mouseY)
                            scrollElement.offset.y = if (total > max) max else total
                        }

                        val current = scrollElement.offset.y
                        val percent = (current / max) * 100.0

                        val maxY = (maxOffset - startOffset) * -1

                        val offsetY = maxY * (percent / 100.0)

                        animate(0.4, Easings.CIRC_OUT) {
                            container.offset.y = -offsetY + startOffset
                        }

                        lasMouseLoc = mouseY
                    }
                } else if (scrollActiveHover) scrollActiveHover = false

                if (wheel != 0 && !mouse) {
                    var start = false
                    var end = false

                    var offsetPlus = wheel / 1.25
                    val offsetOne = container.offset.y + offsetPlus

                    if (maxSize <= sizeY) {
                        offsetPlus = startOffset
                        end = true
                    } else {
                        if (offsetOne >= startOffset) {
                            offsetPlus = startOffset
                            start = true
                        }

                        if (offsetOne <= maxOffset) {
                            offsetPlus = maxOffset
                            end = true
                        }
                    }

                    val setOffset = if (start || end) offsetPlus else container.offset.y + offsetPlus

                    animate(0.4, Easings.CIRC_OUT) {
                        container.offset.y = setOffset
                    }

                    val current = setOffset - startOffset
                    val max = (maxOffset - startOffset)

                    val percent = (current / max) * 100.0

                    val maxY = scrollContainer.size.y - scrollElement.size.y

                    val offsetY = maxY * (percent / 100.0)

                    animate(0.4, Easings.CIRC_OUT) {
                        scrollElement.offset.y = offsetY
                    }
                }
            }
        }

        update()
        updateScale()

        mod.registerHandler<WindowResize> { updateScale(resolution) }
        mod.registerHandler<ScaleChange> { updateScale() }

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

                favoriteCategory.button.color = if (activeCategory == favoriteCategory) Color(21, 53, 98, 1.0)
                else Color(42, 102, 189, 1.0)
            }
            val searchText = search.contentText.content
                .replace("|", "")
                .lowercase()

            if (search.typing && symbolsCount != searchText.length) {
                symbolsCount = searchText.length

                val russian = search.contentText.content
                    .filter { englishSymbols.contains(it) }
                    .map { replacement[replacement.indexOf(it) + 1] }
                val english = search.contentText.content
                    .filter { russianSymbols.contains(it) }
                    .map { replacement[maxOf(0, replacement.indexOf(it))] }

                hoverContainer?.enabled = false

                redraw(symbolsCount == 0) { game ->
                    val title = game.title?.lowercase() ?: ""
                    var sum =
                        if (title.contains(russian.toString()) ||
                            title.contains(english.toString()) ||
                            title.contains(search.contentText.content)
                        ) 7500 * (search.contentText.content.length - 1) else 0

                    sum += if (game.keywords?.contains(searchText) == true) 100000 else 0

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

    private fun update() {
        rootElement?.let { removeChild(it) }

        rootElement = +rectangle {
            val resolution = clientApi.resolution()
            val scale = resolution.scaleFactor

            val scaleHeight = resolution.scaledHeight_double
            val sizeY = (scaleHeight * scale) / 2.0

            size = V3(550.0, sizeY)

            origin = TOP
            align = TOP

            +container
            redraw()

            +header

            onMouseDown {
                if (openned) {
                    dropMenu!!.expanded = false
                    dropMenu!!.content.enabled = false
                }
            }
        }

        hoverContainer = +carved {
            carveSize = 2.0
            color = Color(75, 75, 75)

            enabled = false
            +hoverCenter

            beforeRender { GlStateManager.disableDepth() }
            afterRender { GlStateManager.enableDepth() }
        }

        afterRender {
            if (openned) {
                clientApi.resolution().run {
                    val mouseX = Mouse.getX()
                    val mouseY = Mouse.getY()

                    val displayWidth = Display.getWidth()
                    val displayHeight = Display.getHeight()

                    val sizeX = mouseX / scaleFactor
                    val sizeY = (displayHeight - mouseY) / scaleFactor

                    val container = hoverContainer ?: return@afterRender

                    val outX = sizeX + (container.size.x * container.scale.x) + 15 > displayWidth / scaleFactor
                    val outY = sizeY - (container.size.y * container.scale.y) - 15 > displayHeight / scaleFactor

                    container.offset.x = if (outX) sizeX - (container.size.x * container.scale.x) - 6.0 else sizeX + 6.0
//                    container.offset.y = if (outY) sizeY + (container.size.y * container.scale.y) + 6.0 else sizeX - 6.0
                    container.offset.y = sizeY - 6.0
                }
            }
        }
    }

    fun banners() : List<CompassBanner> {
        var banners = compass.banners

        if (banners != null) {
            banners = banners.filter { it.afterTime.isNotEmpty() && it.beforeTime.isNotEmpty() }
                .filter {
                    val nowSeconds = Instant.now().epochSecond
                    val afterSeconds = OffsetDateTime.parse(it.afterTime).toInstant().epochSecond
                    val beforeSeconds = OffsetDateTime.parse(it.beforeTime).toInstant().epochSecond

                    (afterSeconds <= nowSeconds) && (nowSeconds <= beforeSeconds)
                }

            if (banners.isNotEmpty()) return banners
        }

        return listOf()
    }

    private fun updateScale(resolution: ScaledResolution = clientApi.resolution()) {
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

        val realSizeScrollY = (Display.getHeight() / factor) - 15.0
        val realSizeScrollScale = realSizeScrollY * scaleTotal
        val realSizeInScale = realSizeScrollScale - realSizeScrollY
        val realSizeY = if (factor == 1) realSizeScrollY / scaleTotal else realSizeScrollY - realSizeInScale

        scrollContainer.size.y = realSizeY

        rootElement.size.y = realSizeY
        rootElement.scale = V3(scaleTotal, scaleTotal, 1.0)

        hoverContainer?.scale = V3(scaleTotal, scaleTotal, 1.0)
        scrollContainer.scale = V3(scaleTotal, scaleTotal, 1.0)
    }
}