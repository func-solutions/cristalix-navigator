class Tag(val tag: String, val color: Int) {
    fun getBackground() = mod.hexToColor(color)
}
