package com.example.nocturnevpn.view.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import com.example.nocturnevpn.R
import com.example.nocturnevpn.utils.ConsentManager

/**
 * Consent Popup Dialog for GDPR/CCPA compliance
 * Shows on first app install for regions requiring consent
 */
class ConsentPopupDialog(
    context: Context,
    private val currentConsentStatus: ConsentManager.ConsentStatus,
    private val onConsentResult: (ConsentManager.ConsentStatus) -> Unit
) : Dialog(context) {

    private lateinit var consentRadioGroup: RadioGroup
    private lateinit var personalizedRadio: RadioButton
    private lateinit var nonPersonalizedRadio: RadioButton
    private lateinit var personalizedOption: LinearLayout
    private lateinit var nonPersonalizedOption: LinearLayout
    private lateinit var continueButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.consent_popup_dialog)
        
        // Set dialog properties
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        
        // Check if consent is actually required for this region
        val consentManager = ConsentManager.getInstance(context)
        if (!consentManager.isConsentRequired()) {
            Log.d("ConsentPopupDialog", "📋 Consent not required for this region, dismissing dialog")
            onConsentResult(ConsentManager.ConsentStatus.NOT_REQUIRED)
            dismiss()
            return
        }
        
        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        consentRadioGroup = findViewById(R.id.consentRadioGroup)
        personalizedRadio = findViewById(R.id.personalizedRadio)
        nonPersonalizedRadio = findViewById(R.id.nonPersonalizedRadio)
        personalizedOption = findViewById(R.id.personalizedOption)
        nonPersonalizedOption = findViewById(R.id.nonPersonalizedOption)
        continueButton = findViewById(R.id.continueButton)
        
        // Set default selection based on current consent status
        setDefaultSelection()
        updateOptionSelection()
    }
    
    /**
     * Set default radio button selection based on current consent status
     */
    private fun setDefaultSelection() {
        when (currentConsentStatus) {
            ConsentManager.ConsentStatus.PERSONALIZED -> {
                personalizedRadio.isChecked = true
                nonPersonalizedRadio.isChecked = false
            }
            ConsentManager.ConsentStatus.NON_PERSONALIZED -> {
                nonPersonalizedRadio.isChecked = true
                personalizedRadio.isChecked = false
            }
            ConsentManager.ConsentStatus.NOT_REQUIRED -> {
                // Default to non-personalized for NOT_REQUIRED
                nonPersonalizedRadio.isChecked = true
                personalizedRadio.isChecked = false
            }
            ConsentManager.ConsentStatus.UNKNOWN -> {
                // Default to non-personalized for UNKNOWN
                nonPersonalizedRadio.isChecked = true
                personalizedRadio.isChecked = false
            }
        }
    }

    private fun setupClickListeners() {
        // RadioGroup listener to handle radio button changes
        consentRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.personalizedRadio -> {
                    personalizedRadio.isChecked = true
                    nonPersonalizedRadio.isChecked = false
                }
                R.id.nonPersonalizedRadio -> {
                    nonPersonalizedRadio.isChecked = true
                    personalizedRadio.isChecked = false
                }
            }
            updateOptionSelection()
        }

        // Personalized option click (entire area)
        personalizedOption.setOnClickListener {
            consentRadioGroup.check(R.id.personalizedRadio)
        }

        // Non-personalized option click (entire area)
        nonPersonalizedOption.setOnClickListener {
            consentRadioGroup.check(R.id.nonPersonalizedRadio)
        }

        // Continue button click
        continueButton.setOnClickListener {
            val consentStatus = when {
                personalizedRadio.isChecked -> ConsentManager.ConsentStatus.PERSONALIZED
                nonPersonalizedRadio.isChecked -> ConsentManager.ConsentStatus.NON_PERSONALIZED
                else -> ConsentManager.ConsentStatus.NON_PERSONALIZED
            }
            
            onConsentResult(consentStatus)
            dismiss()
        }
    }

    private fun updateOptionSelection() {
        personalizedOption.isSelected = personalizedRadio.isChecked
        nonPersonalizedOption.isSelected = nonPersonalizedRadio.isChecked
    }
} 