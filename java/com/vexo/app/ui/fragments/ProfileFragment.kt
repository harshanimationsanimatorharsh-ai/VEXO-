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

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvName = view.findViewById<TextView>(R.id.tvUserName)
        val tvEmail = view.findViewById<TextView>(R.id.tvUserEmail)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)
        val btnTelegram = view.findViewById<Button>(R.id.btnTelegram)
        val btnInstagram = view.findViewById<Button>(R.id.btnInstagram)
        val btnSupport = view.findViewById<Button>(R.id.btnSupport)

        // Load user info
        val prefs = requireContext().getSharedPreferences("vexo_prefs", 0)
        tvName?.text = prefs.getString("user_name", "Guest")
        tvEmail?.text = prefs.getString("user_email", "")

        btnLogout?.setOnClickListener {
            prefs.edit()
                .putBoolean("is_logged_in", false)
                .apply()
            val intent = Intent(requireContext(), com.vexo.app.ui.AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        btnTelegram?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Mrharshshrivas")))
        }

        btnInstagram?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/levelupwith.harsh")))
        }

        btnSupport?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:theharshshrivas@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "VEXO Support")
            })
        }
    }
}
