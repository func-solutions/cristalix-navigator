data class CompassBanner(
    val text: List<String>,
    val texture: String? = "",
    val afterTime: String = "",
    val beforeTime: String = "",
    val endedText: String = "Акция закончилась!",
    val timeText: String = "До конца ¨ffca42акции ¨ffffff%time%",
    val timeSlider: Int = 10,
    val button: BannerButton ?= null
)