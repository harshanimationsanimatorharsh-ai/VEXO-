package com.vexo.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.vexo.app.R

class MediaPickerActivity : AppCompatActivity() {

    private val selectedUris = mutableListOf<Uri>()

    private val pickVideos = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNullOrEmpty()) {
            Toast.makeText(this, "No video selected", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        selectedUris.clear()
        selectedUris.addAll(uris)

        // Go directly to editor
        val intent = Intent(this, EditorActivity::class.java)
        intent.putParcelableArrayListExtra("selected_uris", ArrayList(selectedUris))
        startActivity(intent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_picker)

        val btnPickVideo = findViewById<Button>(R.id.btnPickVideo)
        val btnBack = findViewById<Button>(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

        btnPickVideo.setOnClickListener {
            pickVideos.launch("video/*")
        }
    }
}
