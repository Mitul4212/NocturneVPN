package com.nocturnevpn.view.fragment

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.nocturnevpn.R
import com.nocturnevpn.utils.AnimatedBorderManager

class AfterPremiumFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_after_premium, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val premiumMemberText = view.findViewById<TextView>(R.id.tvPremiumMember)
        premiumMemberText?.let { textView ->
            val paint = textView.paint
            val width = paint.measureText(textView.text.toString())
            textView.paint.shader = LinearGradient(
                0f, 0f, width, textView.textSize,
                intArrayOf(Color.parseColor("#6622CC"), Color.parseColor("#22CCC2")),
                null, Shader.TileMode.CLAMP
            )
            textView.invalidate()
        }
        
        // Trigger animated border when user completes premium purchase
        val animatedBorderManager = AnimatedBorderManager.getInstance(requireContext())
        animatedBorderManager.setShouldShowAfterNavigation(true)
    }
} 