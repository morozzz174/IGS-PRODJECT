package ru.company.izhs_planner.mobile_ads

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MobileAdsManager(private val context: Context) {
    private val _isBannerLoaded = MutableStateFlow(false)
    val isBannerLoaded: StateFlow<Boolean> = _isBannerLoaded

    private val _isInterstitialLoaded = MutableStateFlow(false)
    val isInterstitialLoaded: StateFlow<Boolean> = _isInterstitialLoaded

    fun initialize() {
    }

    fun loadBanner(container: FrameLayout): View? {
        return null
    }

    fun showBanner(container: FrameLayout): Boolean {
        return false
    }

    fun hideBanner() {
    }

    fun loadInterstitial() {
    }

    fun showInterstitial(onDismiss: () -> Unit = {}) {
        onDismiss()
    }

    fun destroy() {
    }

    companion object {
        private const val BANNER_AD_UNIT_ID = "demo-banner-yandex"
        private const val INTERSTITIAL_AD_UNIT_ID = "demo-interstitial-yandex"
    }
}
