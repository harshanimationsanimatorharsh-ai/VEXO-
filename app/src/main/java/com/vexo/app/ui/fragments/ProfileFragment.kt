package com.vexo.app.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.vexo.app.databinding.FragmentProfileBinding
import com.vexo.app.ui.AuthActivity

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()

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

        val user = auth.currentUser
        binding.tvName.text = user?.displayName ?: "VEXO User"
        binding.tvEmail.text = user?.email
            ?: user?.phoneNumber ?: "vexo@user.com"

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
            auth.signOut()
            startActivity(Intent(requireContext(), AuthActivity::class.java))
            requireActivity().finishAffinity()
        }

        binding.btnDeleteAccount.setOnClickListener {
            user?.delete()
                ?.addOnSuccessListener {
                    Toast.makeText(requireContext(),
                        "Account deleted", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(requireContext(), AuthActivity::class.java))
                    requireActivity().finishAffinity()
                }
                ?.addOnFailureListener {
                    Toast.makeText(requireContext(),
                        it.message, Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
