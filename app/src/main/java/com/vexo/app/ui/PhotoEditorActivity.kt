package com.vexo.app.ui

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.vexo.app.R

class PhotoEditorActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_editor)

        imageView = findViewById(R.id.ivPhoto)
        val btnBack       = findViewById<ImageButton>(R.id.btnBack)
        val btnOriginal   = findViewById<Button>(R.id.btnFilterOriginal)
        val btnBW         = findViewById<Button>(R.id.btnFilterBW)
        val btnWarm       = findViewById<Button>(R.id.btnFilterWarm)
        val btnCool       = findViewById<Button>(R.id.btnFilterCool)
        val btnVintage    = findViewById<Button>(R.id.btnFilterVintage)
        val sbBrightness  = findViewById<SeekBar>(R.id.sbBrightness)
        val sbSaturation  = findViewById<SeekBar>(R.id.sbSaturation)

        val uriString = intent.getStringExtra("photo_uri")
        if (uriString != null) {
            Glide.with(this).load(Uri.parse(uriString)).into(imageView)
        }

        btnBack?.setOnClickListener { finish() }

        btnOriginal?.setOnClickListener {
            imageView.colorFilter = null
            Toast.makeText(this, "Original", Toast.LENGTH_SHORT).show()
        }

        btnBW?.setOnClickListener {
            val cm = ColorMatrix()
            cm.setSaturation(0f)
            imageView.colorFilter = ColorMatrixColorFilter(cm)
            Toast.makeText(this, "B&W", Toast.LENGTH_SHORT).show()
        }

        btnWarm?.setOnClickListener {
            val cm = ColorMatrix(floatArrayOf(
                1.2f, 0f, 0f, 0f, 20f,
                0f, 1.0f, 0f, 0f, 10f,
                0f, 0f, 0.8f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            ))
            imageView.colorFilter = ColorMatrixColorFilter(cm)
            Toast.makeText(this, "Warm", Toast.LENGTH_SHORT).show()
        }

        btnCool?.setOnClickListener {
            val cm = ColorMatrix(floatArrayOf(
                0.9f, 0f, 0f, 0f, -10f,
                0f, 0.95f, 0f, 0f, 0f,
                0f, 0f, 1.2f, 0f, 20f,
                0f, 0f, 0f, 1f, 0f
            ))
            imageView.colorFilter = ColorMatrixColorFilter(cm)
            Toast.makeText(this, "Cool", Toast.LENGTH_SHORT).show()
        }

        btnVintage?.setOnClickListener {
            val cm = ColorMatrix(floatArrayOf(
                0.9f, 0.05f, 0f, 0f, 20f,
                0.05f, 0.85f, 0f, 0f, 10f,
                0f, 0f, 0.7f, 0f, 20f,
                0f, 0f, 0f, 1f, 0f
            ))
            imageView.colorFilter = ColorMatrixColorFilter(cm)
            Toast.makeText(this, "Vintage", Toast.LENGTH_SHORT).show()
        }

        sbBrightness?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, v: Int, f: Boolean) {
                val b = (v - 50) * 2f
                val cm = ColorMatrix(floatArrayOf(
                    1f, 0f, 0f, 0f, b,
                    0f, 1f, 0f, 0f, b,
                    0f, 0f, 1f, 0f, b,
                    0f, 0f, 0f, 1f, 0f
                ))
                imageView.colorFilter = ColorMatrixColorFilter(cm)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        sbSaturation?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, v: Int, f: Boolean) {
                val sat = v / 50f
                val cm = ColorMatrix()
                cm.setSaturation(sat)
                imageView.colorFilter = ColorMatrixColorFilter(cm)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }
}
