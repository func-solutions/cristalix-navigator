object StringUtils {

    fun getCompleteTime(time: Long): String {
        //86400 День
        //3600 Час
        //60 Минута
        var hours = time.toInt() / 3600
        var remainder = time.toInt() - hours * 3600
        val min = remainder / 60

        remainder -= min * 60

        val sec = remainder
        var day = 0

        if (hours >= 24) {
            day = hours / 24
            hours -= day * 24
        }

        var year = 0
        if (day >= 365) {
            year = day / 365
            day -= year * 365
        }

        val y = "$year " + correctWord(year, listOf("", "год", "года", "лет"))
        val d = "$day " + correctWord(day, listOf("д", "ень", "ня", "ней"))
        val h = "$hours " + correctWord(hours, listOf("час", "", "а", "ов"))
        val m = "$min " + correctWord(min, listOf("минут", "у", "ы", ""))
        val s = "$sec " + correctWord(sec, listOf("секунд", "у", "ы", ""))

        if (year > 0) return y
        if (day > 0) return d
        if (hours > 0) return h
        if (min > 0) return m

        return s
    }

    fun correctWord(time: Int, text: List<String>): String {
        val single = text[0] + text[1]
        val lessFive = text[0] + text[2]
        val others = text[0] + text[3]

        if (time % 100 in 11..14) return others

        if (time % 10 == 1) return single
        if (time % 10 == 2 || time % 10 == 3 || time % 10 == 4) return lessFive

        return others
    }
}