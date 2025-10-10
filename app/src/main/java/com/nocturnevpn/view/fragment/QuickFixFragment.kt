package com.nocturnevpn.view.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nocturnevpn.R
import com.nocturnevpn.databinding.FragmentQuickFixBinding

class QuickFixFragment : Fragment() {
    private var _binding: FragmentQuickFixBinding? = null
    private val binding get() = _binding!!
    private var listener: OnContactUsListener? = null
    private var category: String? = null

    interface OnContactUsListener {
        fun onContactUsClicked()
        fun onCancelQuickFix()
    }

    companion object {
        private const val ARG_CATEGORY = "category"
        fun newInstance(category: String?): QuickFixFragment {
            val fragment = QuickFixFragment()
            val args = Bundle()
            args.putString(ARG_CATEGORY, category)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnContactUsListener) {
            listener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        category = arguments?.getString(ARG_CATEGORY)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentQuickFixBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val quickFixMap = mapOf(
            "Connection Problem" to listOf(
                "1. Ensure your device is connected to the internet (WiFi or Mobile Data).",
                "2. Try toggling Airplane mode ON and OFF.",
                "3. Restart the app and try connecting again.",
                "4. If using WiFi, restart your router.",
                "5. Try switching between WiFi and Mobile Data."
            ),
            "Slow Speed" to listOf(
                "1. Try connecting to a different VPN server closer to your location.",
                "2. Close background apps that may be using bandwidth.",
                "3. Pause any downloads or streaming on your device.",
                "4. Restart your device to clear temporary issues.",
                "5. Test your base internet speed without VPN."
            ),
            "App Crash" to listOf(
                "1. Make sure you are using the latest version of the app from the Play Store.",
                "2. Clear the app cache from your device settings.",
                "3. Restart your device and try again.",
                "4. If the problem persists, uninstall and reinstall the app."
            ),
            "Payment Issue" to listOf(
                "1. Check if your payment method is valid and has sufficient balance.",
                "2. Try using a different payment method if available.",
                "3. Restart the app and attempt the payment again.",
                "4. If you were charged but did not receive service, contact support with your transaction ID."
            ),
            "Login/Signup Issue" to listOf(
                "1. Double-check your email and password for typos.",
                "2. If you forgot your password, use the 'Forgot Password' option.",
                "3. Make sure your internet connection is stable.",
                "4. If signing up, ensure your email is not already registered."
            ),
            "Server Unavailable" to listOf(
                "1. Try connecting to a different server from the list.",
                "2. Wait a few minutes and try again; the server may be under maintenance.",
                "3. Check for app updates that may fix server issues.",
                "4. If the issue persists, contact support with the server name."
            ),
            "Streaming Not Working" to listOf(
                "1. Try switching to a server optimized for streaming (if available).",
                "2. Clear your browser/app cache and cookies.",
                "3. Restart the streaming app or browser.",
                "4. Some streaming services block VPNs; try a different server or contact support."
            ),
            "App UI/UX Issue" to listOf(
                "1. Restart the app to see if the issue resolves.",
                "2. Make sure you are using the latest version of the app.",
                "3. If a button or feature is not working, try reinstalling the app.",
                "4. Send a screenshot and description to support for further help."
            ),
            "Other" to listOf(
                "1. Restart the app and your device.",
                "2. Check for app updates in the Play Store.",
                "3. Contact support with a detailed description of your issue."
            )
        )
        val quickFixes = quickFixMap[category] ?: listOf(
            "Restart the app",
            "Check your internet connection",
            "Update the app",
            "Clear app cache",
            "Reinstall the app"
        )
        val adapter = QuickFixAdapter(quickFixes)
        binding.rvQuickFixes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvQuickFixes.adapter = adapter

        binding.btnContactUs.setOnClickListener {
            listener?.onContactUsClicked()
        }
        binding.btnCancel.setOnClickListener {
            listener?.onCancelQuickFix()
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    class QuickFixAdapter(private val items: List<String>) : RecyclerView.Adapter<QuickFixAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val text: TextView = view.findViewById(R.id.quick_fix_text)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_quick_fix, parent, false)
            return ViewHolder(view)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.text.text = items[position]
        }
        override fun getItemCount() = items.size
    }
} 