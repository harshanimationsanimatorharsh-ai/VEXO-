package com.vexo.app.ui.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.vexo.app.databinding.FragmentHomeBinding
import com.vexo.app.ui.EditorActivity
import com.vexo.app.ui.MediaPickerActivity
import com.vexo.app.ui.PhotoEditorActivity

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val videoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uris = result.data
                ?.getParcelableArrayListExtra<Uri>("selected_media")
            if (!uris.isNullOrEmpty()) {
                val intent = Intent(requireContext(), EditorActivity::class.java)
                intent.putParcelableArrayListExtra("media_uris", uris)
                startActivity(intent)
            }
        }
    }

    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val intent = Intent(requireContext(), PhotoEditorActivity::class.java)
            intent.putExtra("photo_uri", it)
            startActivity(intent)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardNewVideo.setOnClickListener {
            videoPickerLauncher.launch(
                Intent(requireContext(), MediaPickerActivity::class.java)
            )
        }

        binding.cardEditPhoto.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }

        binding.toolAutocut.setOnClickListener {
            videoPickerLauncher.launch(
                Intent(requireContext(), MediaPickerActivity::class.java)
            )
        }

        binding.toolRemoveBg.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }

        binding.toolCaptions.setOnClickListener {
            videoPickerLauncher.launch(
                Intent(requireContext(), MediaPickerActivity::class.java)
            )
        }

        binding.toolEnhance.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
