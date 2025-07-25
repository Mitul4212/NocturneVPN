package com.example.nocturnevpn.view.fragment

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.nocturnevpn.databinding.FragmentEditUserInfoBinding

class EditUserInfoFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentEditUserInfoBinding? = null
    private val binding get() = _binding!!

    private var currentUserName: String = ""
    private var currentUserEmail: String = ""
    private var isUserVerified = false // You can set this based on your user data

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditUserInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get user data from arguments
        arguments?.let { args ->
            currentUserName = args.getString("user_name", "")
            currentUserEmail = args.getString("user_email", "")
        }

        setupUI()
        setupClickListeners()
        updateVerificationUI()
        // No need for custom animation, BottomSheetDialogFragment handles it
    }

    private fun setupUI() {
        // Set current user data
        binding.userNameEditText.setText(currentUserName)
        binding.emailTextView.text = currentUserEmail
    }

    private fun setupClickListeners() {
        // Cancel button
        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        // Save button
        binding.saveButton.setOnClickListener {
            saveUserInfo()
        }

        // Verify button
        binding.verifyButton.setOnClickListener {
            verifyEmail()
        }
    }

    private fun verifyEmail() {
        // Simulate verification process
        isUserVerified = true
        updateVerificationUI()
        Toast.makeText(context, "Email verified successfully!", Toast.LENGTH_SHORT).show()
        // Here you would typically make an API call to verify the user
    }

    // No need for custom animation or overlay logic

    private fun saveUserInfo() {
        val newUserName = binding.userNameEditText.text.toString().trim()
        
        if (newUserName.isEmpty()) {
            binding.userNameInputLayout.error = "Name cannot be empty"
            return
        }

        // Here you would typically save the user info to your database or SharedPreferences
        // For now, we'll just show a success message and dismiss the fragment
        
        // TODO: Implement actual save logic
        // Example: saveUserInfoToDatabase(newUserName, currentUserEmail)
        
        Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
        
        // Notify the parent fragment about the update
        val result = Bundle().apply {
            putString("updated_user_name", newUserName)
            putString("updated_user_email", currentUserEmail)
        }
        // Send result back to parent fragment using the appropriate fragment manager
        if (parentFragmentManager.fragments.contains(this)) {
            // If added as child fragment, send to parent
            parentFragmentManager.setFragmentResult("edit_user_info_result", result)
        } else {
            // If added via navigation, send to activity
            requireActivity().supportFragmentManager.setFragmentResult("edit_user_info_result", result)
        }
        dismiss()
    }

    private fun updateVerificationUI() {
        if (isUserVerified) {
            binding.verifyButton.visibility = View.GONE
            binding.tickIcon.visibility = View.VISIBLE
        } else {
            binding.verifyButton.visibility = View.VISIBLE
            binding.tickIcon.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(userName: String, userEmail: String): EditUserInfoFragment {
            return EditUserInfoFragment().apply {
                arguments = Bundle().apply {
                    putString("user_name", userName)
                    putString("user_email", userEmail)
                }
            }
        }
    }
} 