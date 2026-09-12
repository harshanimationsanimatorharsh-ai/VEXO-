package com.vexo.app.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vexo.app.databinding.ActivityMediaPickerBinding
import com.vexo.app.databinding.ItemMediaBinding

class MediaPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMediaPickerBinding
    private val selected = ArrayList<Uri>()
    private val allMedia = mutableListOf<Uri>()
    private lateinit var adapter: MediaAdapter

    companion object {
        private const val PERMISSION_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = MediaAdapter(allMedia, selected) { updateBtn() }
        binding.rvMedia.layoutManager = GridLayoutManager(this, 3)
        binding.rvMedia.adapter = adapter

        checkPermissionsAndLoad()

        binding.ivBack.setOnClickListener { finish() }

        binding.btnSelectAll.setOnClickListener {
            if (selected.size == allMedia.size) {
                selected.clear()
            } else {
                selected.clear()
                selected.addAll(allMedia)
            }
            adapter.notifyDataSetChanged()
            updateBtn()
        }

        binding.btnAdd.setOnClickListener {
            if (selected.isEmpty()) {
                Toast.makeText(this,
                    "Select at least 1 media",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            setResult(Activity.RESULT_OK,
                Intent().putParcelableArrayListExtra(
                    "selected_media", selected))
            finish()
        }
    }

    private fun checkPermissionsAndLoad() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val imgPerm = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_MEDIA_IMAGES)
            val vidPerm = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_MEDIA_VIDEO)
            if (imgPerm != PackageManager.PERMISSION_GRANTED ||
                vidPerm != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO
                    ), PERMISSION_REQUEST)
            } else {
                loadMedia()
            }
        } else {
            val perm = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE)
            if (perm != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST)
            } else {
                loadMedia()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMedia()
            } else {
                Toast.makeText(this,
                    "Permission required to access media",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadMedia() {
        allMedia.clear()

        // Load Videos
        val videoCursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Video.Media._ID),
            null, null,
            MediaStore.Video.Media.DATE_MODIFIED + " DESC"
        )
        videoCursor?.use {
            val idCol = it.getColumnIndexOrThrow(
                MediaStore.Video.Media._ID)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                allMedia.add(Uri.withAppendedPath(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id.toString()))
            }
        }

        // Load Images
        val imageCursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID),
            null, null,
            MediaStore.Images.Media.DATE_MODIFIED + " DESC"
        )
        imageCursor?.use {
            val idCol = it.getColumnIndexOrThrow(
                MediaStore.Images.Media._ID)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                allMedia.add(Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()))
            }
        }

        if (allMedia.isEmpty()) {
            Toast.makeText(this,
                "No media found on device",
                Toast.LENGTH_SHORT).show()
        }

        adapter.notifyDataSetChanged()
    }

    private fun updateBtn() {
        val n = selected.size
        binding.btnAdd.text = if (n > 0) "Add ($n)" else "Add"
    }

    inner class MediaAdapter(
        private val items: List<Uri>,
        private val sel: ArrayList<Uri>,
        private val onChange: () -> Unit
    ) : RecyclerView.Adapter<MediaAdapter.VH>() {

        inner class VH(val b: ItemMediaBinding) :
            RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(p: ViewGroup, t: Int) =
            VH(ItemMediaBinding.inflate(
                LayoutInflater.from(p.context), p, false))

        override fun onBindViewHolder(h: VH, pos: Int) {
            val uri = items[pos]
            Glide.with(h.itemView)
                .load(uri)
                .centerCrop()
                .placeholder(android.R.color.darker_gray)
                .into(h.b.ivThumbnail)

            val isSelected = sel.contains(uri)
            h.b.viewSelected.alpha = if (isSelected) 1f else 0f
            h.b.tvOrder.text = if (isSelected)
                (sel.indexOf(uri) + 1).toString() else ""
            h.b.tvOrder.alpha = if (isSelected) 1f else 0f

            h.itemView.setOnClickListener {
                if (sel.contains(uri)) sel.remove(uri)
                else sel.add(uri)
                notifyDataSetChanged()
                onChange()
            }
        }

        override fun getItemCount() = items.size
    }
}
