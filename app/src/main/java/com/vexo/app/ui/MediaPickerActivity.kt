package com.vexo.app.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = MediaAdapter(allMedia, selected) { updateBtn() }
        binding.rvMedia.layoutManager = GridLayoutManager(this, 3)
        binding.rvMedia.adapter = adapter

        loadMedia()

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
                    "Select at least 1 media", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            setResult(Activity.RESULT_OK,
                Intent().putParcelableArrayListExtra("selected_media", selected))
            finish()
        }
    }

    private fun loadMedia() {
        val cursor = contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.MEDIA_TYPE
            ),
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=" +
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + " OR " +
                    MediaStore.Files.FileColumns.MEDIA_TYPE + "=" +
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO,
            null,
            MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC"
        )
        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val typeCol = it.getColumnIndexOrThrow(
                MediaStore.Files.FileColumns.MEDIA_TYPE)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val type = it.getInt(typeCol)
                allMedia.add(
                    if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)
                        Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id.toString())
                    else
                        Uri.withAppendedPath(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            id.toString())
                )
            }
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
            Glide.with(h.itemView).load(uri)
                .centerCrop().into(h.b.ivThumbnail)
            val isSelected = sel.contains(uri)
            h.b.viewSelected.alpha = if (isSelected) 1f else 0f
            h.b.tvOrder.text = if (isSelected)
                (sel.indexOf(uri) + 1).toString() else ""
            h.b.tvOrder.alpha = if (isSelected) 1f else 0f
            h.itemView.setOnClickListener {
                if (sel.contains(uri)) sel.remove(uri) else sel.add(uri)
                notifyDataSetChanged()
                onChange()
            }
        }

        override fun getItemCount() = items.size
    }
}
