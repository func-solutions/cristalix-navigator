data class Compass(
    val categories: List<CompassGameCategory>,
    val tags: List<Tag>,
    val banners: List<CompassBanner> ?= listOf(),
    val games: List<CompassGame>,
    val globalOnline: Int ?= 0
)
