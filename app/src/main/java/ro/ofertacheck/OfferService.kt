package ro.ofertacheck

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
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
    private var overlay: TextView? = null
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
        val min = getSharedPreferences("cfg", MODE_PRIVATE).getFloat("min_per_km", 2.0f)
        val good = offer.perKm >= min
        val hour = offer.perHour?.let { "\n%.0f lei/oră".format(it) } ?: ""
        showOverlay("%.2f lei/km%s\n%.1f km total".format(offer.perKm, hour, offer.totalKm),
            if (good) Color.parseColor("#2E7D32") else Color.parseColor("#C62828"))
    }

    private fun showOverlay(msg: String, color: Int) = main.post {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        if (overlay == null) {
            overlay = TextView(this).apply {
                setTextColor(Color.WHITE); textSize = 18f; setPadding(32, 20, 32, 20)
            }
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = 120 }
            wm.addView(overlay, lp)
        }
        overlay?.text = msg
        overlay?.setBackgroundColor(color)
        main.removeCallbacks(hideRunnable)
        main.postDelayed(hideRunnable, 8000)
    }

    private fun hideOverlay() {
        overlay?.let { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it) }
        overlay = null
    }

    override fun onInterrupt() { hideOverlay() }
}
