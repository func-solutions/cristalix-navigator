class PlayerData(var favorite: Array<String>) {
    fun removeFavorite(realm: String) {
        favorite = favorite.filter { it != realm }.toTypedArray()
    }

    fun addFavorite(realm: String) {
        favorite = favorite.plus(realm)
    }
}
