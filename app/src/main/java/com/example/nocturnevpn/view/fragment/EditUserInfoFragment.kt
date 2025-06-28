package com.example.nocturnevpn.view.fragment

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.navigation.fragment.findNavController
import com.example.nocturnevpn.databinding.FragmentEditUserInfoBinding

class EditUserInfoFragment : Fragment() {

    private var _binding: FragmentEditUserInfoBinding? = null
    private val binding get() = _binding!!

    private var currentUserName: String = ""
    private var currentUserEmail: String = ""
    private var isUserVerified = false // You can set this based on your user data

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
        startSlideUpAnimation()
    }

    private fun setupUI() {
        // Set current user data
        binding.userNameEditText.setText(currentUserName)
        binding.emailTextView.text = currentUserEmail
    }

    private fun setupClickListeners() {
        // Overlay click to dismiss
        binding.overlay.setOnClickListener {
            dismissWithAnimation()
        }

        // Cancel button
        binding.cancelButton.setOnClickListener {
            dismissWithAnimation()
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

    private fun startSlideUpAnimation() {
        // Start with the container below the screen, invisible and scaled down
        binding.mainContainer.apply {
            translationY = height.toFloat()
            alpha = 0f
            scaleX = 0.95f
            scaleY = 0.95f
        }
        binding.overlay.alpha = 0f

        // Animate the overlay fade in
        val overlayAnimator = ObjectAnimator.ofFloat(binding.overlay, "alpha", 0f, 1f).apply {
            duration = 250
            interpolator = FastOutSlowInInterpolator()
        }

        // Animate the container slide up, fade in, and scale up
        binding.mainContainer.animate()
            .translationY(0f)
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .setInterpolator(FastOutSlowInInterpolator())
            .start()
        overlayAnimator.start()
    }

    private fun dismissWithAnimation() {
        // Animate the overlay fade out
        val overlayAnimator = ObjectAnimator.ofFloat(binding.overlay, "alpha", 1f, 0f).apply {
            duration = 200
            interpolator = FastOutSlowInInterpolator()
        }

        // Animate the container slide down, fade out, and scale down
        binding.mainContainer.animate()
            .translationY(binding.mainContainer.height.toFloat())
            .alpha(0f)
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(300)
            .setInterpolator(FastOutSlowInInterpolator())
            .withEndAction {
                // Use the appropriate fragment manager based on how the fragment was added
                if (parentFragmentManager.fragments.contains(this@EditUserInfoFragment)) {
                    parentFragmentManager.beginTransaction()
                        .remove(this@EditUserInfoFragment)
                        .commit()
                } else {
                    // Fallback to pop back stack if added via navigation
                    findNavController().navigateUp()
                }
            }
            .start()
        overlayAnimator.start()
    }

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
        
        dismissWithAnimation()
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