package ru.company.izhs_planner.mobile_ads

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import com.yandex.mobile.ads.common.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MobileAdsManager(private val context: Context) {
    private var banner: AdView? = null
    private var interstitial: InterstitialAd? = null
    private var isLoading = false
    
    private val _isBannerLoaded = MutableStateFlow(false)
    val isBannerLoaded: StateFlow<Boolean> = _isBannerLoaded
    
    private val _isInterstitialLoaded = MutableStateFlow(false)
    val isInterstitialLoaded: StateFlow<Boolean> = _isInterstitialLoaded
    
    fun initialize() {
        MobileAds.configure(context) {
            this.withOnAdLoadedListener {
                _isBannerLoaded.value = true
            }
            this.withOnAdFailedToLoadListener { _, _ ->
                _isBannerLoaded.value = false
            }
        }
    }
    
    fun loadBanner(container: FrameLayout): View? {
        if (isDebugBuild()) return null
        
        try {
            banner = AdView(context).apply {
                setAdUnitId(BANNER_AD_UNIT_ID)
                setAdSize(AdSize.BANNER_320x50)
            }
            
            banner?.loadAd(AdRequest.Builder().build())
            container.addView(banner)
            
            return banner
        } catch (e: Exception) {
            return null
        }
    }
    
    fun showBanner(container: FrameLayout): Boolean {
        if (_isBannerLoaded.value && banner != null) {
            banner?.visibility = View.VISIBLE
            return true
        }
        return false
    }
    
    fun hideBanner() {
        banner?.visibility = View.GONE
    }
    
    fun loadInterstitial() {
        if (isDebugBuild() || isLoading) return
        isLoading = true
        
        try {
            InterstitialAd.loadAd(
                context,
                INTERSTITIAL_AD_UNIT_ID,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadListener {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        interstitial = interstitialAd
                        _isInterstitialLoaded.value = true
                        isLoading = false
                    }
                    
                    override fun onAdFailedToLoad(adError: AdLoadError) {
                        _isInterstitialLoaded.value = false
                        isLoading = false
                    }
                }
            )
        } catch (e: Exception) {
            isLoading = false
        }
    }
    
    fun showInterstitial(onDismiss: () -> Unit = {}) {
        if (!isDebugBuild() && _isInterstitialLoaded.value && interstitial != null) {
            interstitial?.setAdEventListener(object : AdEventListener {
                override fun onAdShown() {}
                override fun onAdFailedToShow(adError: AdShowError) {
                    onDismiss()
                }
                override fun onAdDismissed() {
                    onDismiss()
                    interstitial = null
                    _isInterstitialLoaded.value = false
                }
                override fun onAdClicked() {}
                override fun onLeftApplication() {}
                override fun onReturnedToApplication() {}
                override fun onImpression(impressionData: ImpressionData?) {}
            })
            interstitial?.show()
        } else {
            onDismiss()
        }
    }
    
    fun destroy() {
        banner?.destroy()
        banner = null
        interstitial?.destroy()
        interstitial = null
    }
    
    private fun isDebugBuild(): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            return packageInfo.applicationInfo?.flags?.and(android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            return false
        }
    }
    
    companion object {
        private const val BANNER_AD_UNIT_ID = "demo-banner-yandex"
        private const val INTERSTITIAL_AD_UNIT_ID = "demo-interstitial-yandex"
    }
}