package com.nocturnevpn.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ThemeViewModel : ViewModel() {

    val triggerMapThemeChange = MutableLiveData<Unit>()

    fun notifyThemeChange() {
        triggerMapThemeChange.value = Unit
    }

}