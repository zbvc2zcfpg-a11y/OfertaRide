package ro.ofertacheck

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.*

class CarSettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("cfg", MODE_PRIVATE)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 96, 48, 48) }

        fun label(t: String) = TextView(this).apply { text = t; textSize = 14f; setPadding(0, 32, 0, 8) }
        fun numField(key: String, def: Float) = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(prefs.getFloat(key, def).toString())
        }

        col.addView(TextView(this).apply { text = "Mașina ta"; textSize = 20f })

        col.addView(label("Consum în oraș (l/100km)"))
        val consOras = numField("l100_oras", 7.0f)
        col.addView(consOras, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        col.addView(label("Consum la drum lung (l/100km)"))
        val consDrum = numField("l100_drum", 5.5f)
        col.addView(consDrum, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        col.addView(label("Preț combustibil (lei/litru)"))
        val pret = numField("pret_litru", 7.5f)
        col.addView(pret, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        col.addView(Button(this).apply {
            text = "Salvează"
            setOnClickListener {
                val o = consOras.text.toString().toFloatOrNull()
                val d = consDrum.text.toString().toFloatOrNull()
                val p = pret.text.toString().toFloatOrNull()
                if (o == null || d == null || p == null) {
                    Toast.makeText(context, "Completează toate câmpurile", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                prefs.edit().putFloat("l100_oras", o).putFloat("l100_drum", d).putFloat("pret_litru", p).apply()
                Toast.makeText(context, "Salvat", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
        setContentView(col)
    }
}
