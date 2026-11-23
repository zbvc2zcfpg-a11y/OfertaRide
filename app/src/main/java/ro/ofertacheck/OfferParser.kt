package ro.ofertacheck

data class Offer(val price: Double, val pickupKm: Double, val tripKm: Double, val totalMin: Double?) {
    val totalKm get() = pickupKm + tripKm
    val perKm get() = price / totalKm
    val perHour get() = totalMin?.takeIf { it > 0 }?.let { price / (it / 60.0) }
}

object OfferParser {
    private val priceRx = Regex("""(?:RON|lei)\s?(\d+(?:[.,]\d{1,2})?)|(\d+(?:[.,]\d{1,2})?)\s?(?:RON|lei)""", RegexOption.IGNORE_CASE)
    private val kmRx = Regex("""(\d+(?:[.,]\d+)?)\s?km""", RegexOption.IGNORE_CASE)
    private val minRx = Regex("""(\d+)\s?min""", RegexOption.IGNORE_CASE)

    private fun num(s: String) = s.replace(',', '.').toDouble()

    fun parse(text: String): Offer? {
        val pm = priceRx.find(text) ?: return null
        val price = num(pm.groupValues[1].ifEmpty { pm.groupValues[2] })
        val kms = kmRx.findAll(text).map { num(it.groupValues[1]) }.toList()
        if (kms.size < 2 || price <= 0) return null
        val mins = minRx.findAll(text).map { it.groupValues[1].toDouble() }.toList()
        val totalMin = if (mins.size >= 2) mins[0] + mins[1] else null
        // Primul km = până la preluare, al doilea = cursa
        return Offer(price, kms[0], kms[1], totalMin)
    }
}
