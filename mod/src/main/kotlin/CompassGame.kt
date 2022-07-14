import dev.xdark.clientapi.resource.ResourceLocation

data class CompassGame(
    val name: String? = null,
    val title: String? = null,
    var icon: String? = null,
    val realmType: String? = null,
    var lobby: Boolean = false,
    var subGames: List<CompassGame>? = null,
    var tags: List<String>? = listOf(),
    var backgroundColor: Int = 0,
    var starred: Boolean = false,
    var dynamic: Boolean = false,
    var depend: String? = null,
    var image: ResourceLocation = loading,
    var description: List<String>? = listOf()
) {

    var parent: CompassGame? = null

    var online: Int = 0
    set(value) {
        compassGui?.games?.find { it.compassGame == this }?.online?.content = "Онлайн $value"
        field = value
    }
}
