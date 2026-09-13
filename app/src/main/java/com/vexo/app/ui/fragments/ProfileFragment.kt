package com.vexo.app.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.vexo.app.R
import com.vexo.app.ui.AuthActivity

class ProfileFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireActivity().getSharedPreferences("vexo_prefs", android.content.Context.MODE_PRIVATE)
        view.findViewById<TextView>(R.id.tvUserName)?.text  = prefs.getString("user_name", "VEXO User")
        view.findViewById<TextView>(R.id.tvUserEmail)?.text = prefs.getString("user_email", "")

        view.findViewById<Button>(R.id.btnEmail)?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:theharshshrivas@gmail.com")))
        }
        view.findViewById<Button>(R.id.btnTelegram)?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Mrharshshrivas")))
        }
        view.findViewById<Button>(R.id.btnInstagram)?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/levelupwith.harsh")))
        }
        view.findViewById<Button>(R.id.btnSignOut)?.setOnClickListener {
            prefs.edit().clear().apply()
            startActivity(Intent(requireContext(), AuthActivity::class.java))
            requireActivity().finishAffinity()
        }
    }
}
