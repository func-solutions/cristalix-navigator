data class Tag(
    var tag: String,
    var color: Int
) {
    fun getBackground() = mod.hexToColor(color)
}
