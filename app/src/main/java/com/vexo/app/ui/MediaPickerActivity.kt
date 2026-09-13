package com.vexo.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vexo.app.R

class MediaPickerActivity : AppCompatActivity() {

    private val selectedUris = mutableListOf<Uri>()
    private lateinit var adapter: MediaAdapter
    private val PERM_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_picker)

        val recyclerView = findViewById<RecyclerView>(R.id.rvMedia)
        val btnDone      = findViewById<Button>(R.id.btnDone)
        val btnBack      = findViewById<ImageButton>(R.id.btnBack)

        btnBack?.setOnClickListener { finish() }

        recyclerView?.layoutManager = GridLayoutManager(this, 3)
        adapter = MediaAdapter(emptyList()) { uri, selected ->
            if (selected) selectedUris.add(uri) else selectedUris.remove(uri)
            btnDone?.text = "Done (${selectedUris.size})"
        }
        recyclerView?.adapter = adapter

        btnDone?.setOnClickListener {
            if (selectedUris.isEmpty()) {
                Toast.makeText(this, "Select at least one media", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, EditorActivity::class.java)
            intent.putStringArrayListExtra("media_uris", ArrayList(selectedUris.map { it.toString() }))
            startActivity(intent)
            finish()
        }

        checkPermissions()
    }

    private fun checkPermissions() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES)
        else
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) loadMedia()
        else ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERM_CODE)
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        if (results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED) loadMedia()
        else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
    }

    private fun loadMedia() {
        val uris = mutableListOf<Uri>()
        val projection = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.MEDIA_TYPE)
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}, ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})"
        val cursor = contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection, selection, null,
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        )
        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val typeCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val type = it.getInt(typeCol)
                val contentUri = if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                uris.add(Uri.withAppendedPath(contentUri, id.toString()))
            }
        }
        adapter.updateData(uris)
    }

    // ── Adapter ───────────────────────────────────────────────────
    inner class MediaAdapter(
        private var items: List<Uri>,
        private val onSelect: (Uri, Boolean) -> Unit
    ) : RecyclerView.Adapter<MediaAdapter.VH>() {

        private val selected = mutableSetOf<Uri>()

        fun updateData(newItems: List<Uri>) {
            items = newItems
            notifyDataSetChanged()
        }

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewById(R.id.ivMedia)
            val check: ImageView = view.findViewById(R.id.ivCheck)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_media, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val uri = items[position]
            Glide.with(holder.image.context).load(uri).centerCrop().into(holder.image)
            holder.check.visibility = if (uri in selected) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener {
                if (uri in selected) {
                    selected.remove(uri)
                    onSelect(uri, false)
                } else {
                    selected.add(uri)
                    onSelect(uri, true)
                }
                notifyItemChanged(position)
            }
        }

        override fun getItemCount() = items.size
    }
}
