    private fun handleText(text: String) {
        val offer = OfferParser.parse(text) ?: return
        val prefs = getSharedPreferences("cfg", MODE_PRIVATE)
        val min = prefs.getFloat("min_per_km", 2.0f)
        val good = offer.perKm >= min

        // cost combustibil: folosim consumul "oraș" ca estimare implicită
        val l100 = prefs.getFloat("l100_oras", 7.0f)
        val pretLitru = prefs.getFloat("pret_litru", 7.5f)
        val costCombustibil = offer.totalKm * (l100 / 100.0) * pretLitru
        val profitNet = offer.price - costCombustibil

        val hour = offer.perHour?.let { "%.0f lei/oră".format(it) } ?: "-"
        showOverlay(
            good = good,
            main = "%.2f lei/km".format(offer.perKm),
            detail = "Profit net: %.1f lei  •  %.1f km  •  %s".format(profitNet, offer.totalKm, hour)
        )
    }
