package com.vexo.app.ui

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.vexo.app.databinding.ActivityPhotoEditorBinding

class PhotoEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoEditorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uri = intent.getParcelableExtra<Uri>("photo_uri")
        uri?.let { Glide.with(this).load(it).into(binding.ivPhoto) }

        binding.ivBack.setOnClickListener { finish() }

        setupFilters()
        setupAdjust()

        binding.btnSave.setOnClickListener {
            Toast.makeText(this, "✅ Saved!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupFilters() {
        binding.btnFilterNone.setOnClickListener {
            binding.ivPhoto.clearColorFilter()
        }
        binding.btnFilterBW.setOnClickListener {
            binding.ivPhoto.colorFilter = ColorMatrixColorFilter(
                ColorMatrix().apply { setSaturation(0f) })
        }
        binding.btnFilterWarm.setOnClickListener {
            binding.ivPhoto.colorFilter = ColorMatrixColorFilter(
                ColorMatrix().apply {
                    set(floatArrayOf(
                        1.2f,0f,0f,0f,20f,
                        0f,1f,0f,0f,0f,
                        0f,0f,0.8f,0f,-20f,
                        0f,0f,0f,1f,0f))
                })
        }
        binding.btnFilterCool.setOnClickListener {
            binding.ivPhoto.colorFilter = ColorMatrixColorFilter(
                ColorMatrix().apply {
                    set(floatArrayOf(
                        0.8f,0f,0f,0f,-20f,
                        0f,1f,0f,0f,0f,
                        0f,0f,1.2f,0f,20f,
                        0f,0f,0f,1f,0f))
                })
        }
        binding.btnFilterVintage.setOnClickListener {
            binding.ivPhoto.colorFilter = ColorMatrixColorFilter(
                ColorMatrix().apply {
                    set(floatArrayOf(
                        0.9f,0.1f,0f,0f,20f,
                        0.1f,0.8f,0.1f,0f,10f,
                        0f,0.1f,0.7f,0f,-10f,
                        0f,0f,0f,1f,0f))
                })
        }
    }

    private fun setupAdjust() {
        binding.seekBrightness.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    sb: SeekBar?, p: Int, f: Boolean) {
                    val b = (p - 50) / 50f * 255
                    binding.ivPhoto.colorFilter = ColorMatrixColorFilter(
                        ColorMatrix().apply {
                            set(floatArrayOf(
                                1f,0f,0f,0f,b,
                                0f,1f,0f,0f,b,
                                0f,0f,1f,0f,b,
                                0f,0f,0f,1f,0f))
                        })
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })

        binding.seekSaturation.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    sb: SeekBar?, p: Int, f: Boolean) {
                    binding.ivPhoto.colorFilter = ColorMatrixColorFilter(
                        ColorMatrix().apply { setSaturation(p / 50f) })
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })

        binding.btnReset.setOnClickListener {
            binding.ivPhoto.clearColorFilter()
            binding.seekBrightness.progress = 50
            binding.seekSaturation.progress = 50
        }
    }
}
