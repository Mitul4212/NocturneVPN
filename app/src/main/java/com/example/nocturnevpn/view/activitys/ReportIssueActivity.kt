package com.example.nocturnevpn.view.activitys

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.nocturnevpn.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.progressindicator.LinearProgressIndicator
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.nocturnevpn.view.fragment.IssueCategoryFragment
import com.example.nocturnevpn.view.fragment.QuickFixFragment
import com.example.nocturnevpn.view.fragment.ReportFormFragment

class ReportIssueActivity : AppCompatActivity(),
    IssueCategoryFragment.OnIssueCategorySelectedListener,
    QuickFixFragment.OnContactUsListener {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var stepIndicator: TextView
    private var currentStep = 1
    private var selectedCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_issue)

        toolbar = findViewById(R.id.toolbar)
        progressBar = findViewById(R.id.progress_bar)
        stepIndicator = findViewById(R.id.step_indicator)

        toolbar.setNavigationOnClickListener {
            if (currentStep == 1) {
                finish()
            } else {
                showStep(currentStep - 1)
            }
        }

        if (savedInstanceState == null) {
            showStep(1)
        }
    }

    private fun showStep(step: Int) {
        currentStep = step
        when (step) {
            1 -> {
                replaceFragment(IssueCategoryFragment())
                progressBar.setProgressCompat(33, true)
                stepIndicator.text = "Step 1 of 3"
            }
            2 -> {
                val quickFixFragment = QuickFixFragment.newInstance(selectedCategory)
                replaceFragment(quickFixFragment)
                progressBar.setProgressCompat(66, true)
                stepIndicator.text = "Step 2 of 3"
            }
            3 -> {
                val reportFormFragment = ReportFormFragment.newInstance(selectedCategory)
                replaceFragment(reportFormFragment)
                progressBar.setProgressCompat(100, true)
                stepIndicator.text = "Step 3 of 3"
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_view, fragment)
            .commit()
    }

    // Callbacks from fragments
    override fun onIssueCategorySelected(category: String) {
        selectedCategory = category
        showStep(2)
    }

    override fun onContactUsClicked() {
        showStep(3)
    }

    override fun onBackPressed() {
        if (currentStep == 1) {
            super.onBackPressed()
        } else {
            showStep(currentStep - 1)
        }
    }

    override fun onCancelQuickFix() {
        showStep(1)
    }
} 