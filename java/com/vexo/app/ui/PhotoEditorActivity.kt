package com.vexo.app.ui

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.vexo.app.R

class PhotoEditorActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private var currentUri: Uri? = null

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            currentUri = uri
            imageView.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_editor)

        imageView = findViewById(R.id.photoView)

        val btnPickPhoto = findViewById<Button>(R.id.btnPickPhoto)
        val btnBW = findViewById<Button>(R.id.btnBW)
        val btnBright = findViewById<Button>(R.id.btnBright)
        val btnContrast = findViewById<Button>(R.id.btnContrast)
        val btnReset = findViewById<Button>(R.id.btnReset)
        val btnBack = findViewById<Button>(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        btnPickPhoto.setOnClickListener {
            pickImage.launch("image/*")
        }

        btnBW.setOnClickListener {
            if (currentUri == null) {
                Toast.makeText(this, "Pick a photo first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val matrix = ColorMatrix()
            matrix.setSaturation(0f)
            imageView.colorFilter = ColorMatrixColorFilter(matrix)
        }

        btnBright.setOnClickListener {
            if (currentUri == null) {
                Toast.makeText(this, "Pick a photo first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val matrix = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, 50f,
                0f, 1f, 0f, 0f, 50f,
                0f, 0f, 1f, 0f, 50f,
                0f, 0f, 0f, 1f, 0f
            ))
            imageView.colorFilter = ColorMatrixColorFilter(matrix)
        }

        btnContrast.setOnClickListener {
            if (currentUri == null) {
                Toast.makeText(this, "Pick a photo first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val scale = 1.5f
            val translate = (-0.5f * scale + 0.5f) * 255f
            val matrix = ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            imageView.colorFilter = ColorMatrixColorFilter(matrix)
        }

        btnReset.setOnClickListener {
            imageView.colorFilter = null
        }
    }
}
