package com.vexo.app.ui.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.vexo.app.databinding.FragmentProfileBinding
import com.vexo.app.ui.AuthActivity

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext()
            .getSharedPreferences("vexo_prefs", Context.MODE_PRIVATE)

        binding.tvName.text = prefs.getString("user_name", "VEXO User")
        binding.tvEmail.text = prefs.getString("user_email", "user@vexo.app")

        binding.btnEmail.setOnClickListener {
            Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:theharshshrivas@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "VEXO Support")
                startActivity(Intent.createChooser(this, "Send Email"))
            }
        }

        binding.btnTelegram.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://t.me/Mrharshshrivas")))
        }

        binding.btnInstagram.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://instagram.com/levelupwith.harsh.")))
        }

        binding.btnSignOut.setOnClickListener {
            prefs.edit()
                .putBoolean("is_logged_in", false)
                .putString("user_name", "")
                .putString("user_email", "")
                .apply()
            startActivity(Intent(requireContext(), AuthActivity::class.java))
            requireActivity().finishAffinity()
        }

        binding.btnDeleteAccount.setOnClickListener {
            prefs.edit().clear().apply()
            Toast.makeText(requireContext(),
                "Account deleted", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), AuthActivity::class.java))
            requireActivity().finishAffinity()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
