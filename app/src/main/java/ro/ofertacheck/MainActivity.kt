package ro.ofertacheck

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("cfg", MODE_PRIVATE)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 96, 48, 48) }
        col.addView(TextView(this).apply {
            text = "OfertaCheck\n\n1. Activează serviciul în Accesibilitate.\n2. Setează pragul minim (lei/km).\n3. Setează mașina ta (consum + preț combustibil)."
            textSize = 16f
        })
        col.addView(Button(this).apply {
            text = "Deschide setări Accesibilitate"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        col.addView(Button(this).apply {
            text = "Setează mașina ta"
            setOnClickListener { startActivity(Intent(this@MainActivity, CarSettingsActivity::class.java)) }
        })
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(prefs.getFloat("min_per_km", 2.0f).toString())
        }
        col.addView(TextView(this).apply { text = "Prag minim (lei/km):"; textSize = 14f; setPadding(0, 32, 0, 8) })
        col.addView(input, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        col.addView(Button(this).apply {
            text = "Salvează prag"
            setOnClickListener {
                val v = input.text.toString().toFloatOrNull() ?: return@setOnClickListener
                prefs.edit().putFloat("min_per_km", v).apply()
                Toast.makeText(context, "Salvat: $v lei/km", Toast.LENGTH_SHORT).show()
            }
        })
        setContentView(col)
    }
}
