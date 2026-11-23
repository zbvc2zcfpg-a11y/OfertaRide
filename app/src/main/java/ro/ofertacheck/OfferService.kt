package ro.ofertacheck

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import android.widget.TextView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

class OfferService : AccessibilityService() {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var lastShot = 0L
    private var card: LinearLayout? = null
    private var statusView: TextView? = null
    private var mainView: TextView? = null
    private var detailView: TextView? = null
    private val hideRunnable = Runnable { hideOverlay() }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val now = System.currentTimeMillis()
        if (now - lastShot < 1500) return
        lastShot = now
        takeScreenshot(Display.DEFAULT_DISPLAY, executor, object : TakeScreenshotCallback {
            override fun onSuccess(result: ScreenshotResult) {
                val hw = Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace)
                val bmp = hw?.copy(Bitmap.Config.ARGB_8888, false)
                result.hardwareBuffer.close()
                if (bmp == null) return
                recognizer.process(InputImage.fromBitmap(bmp, 0))
                    .addOnSuccessListener { handleText(it.text) }
            }
            override fun onFailure(errorCode: Int) {}
        })
    }

    private fun handleText(text: String) {
        val offer = OfferParser.parse(text) ?: return
        val prefs = getSharedPreferences("cfg", MODE_PRIVATE)
        val min = prefs.getFloat("min_per_km", 2.0f)
        val good = offer.perKm >= min

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

    private fun showOverlay(good: Boolean, main: String, detail: String) = this.main.post {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val accent = if (good) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
        if (card == null) {
            val bg = GradientDrawable().apply {
                cornerRadius = 28f
                setColor(Color.parseColor("#CC1A1A1A"))
            }
            card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = bg
                setPadding(40, 24, 40, 24)
            }
            statusView = TextView(this).apply { textSize = 14f; setTypeface(typeface, android.graphics.Typeface.BOLD) }
            mainView = TextView(this).apply { textSize = 26f; setTypeface(typeface, android.graphics.Typeface.BOLD); setTextColor(Color.WHITE) }
            detailView = TextView(this).apply { textSize = 13f; setTextColor(Color.parseColor("#DDDDDD")) }
            card!!.addView(statusView)
            card!!.addView(mainView)
            card!!.addView(detailView)
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = 140 }
            wm.addView(card, lp)
        }
        statusView?.text = if (good) "✓ BUNĂ" else "✗ SLABĂ"
        statusView?.setTextColor(accent)
        mainView?.text = main
        detailView?.text = detail
        this.main.removeCallbacks(hideRunnable)
        this.main.postDelayed(hideRunnable, 10000)
    }

    private fun hideOverlay() {
        card?.let { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it) }
        card = null
    }

    override fun onInterrupt() { hideOverlay() }
}
