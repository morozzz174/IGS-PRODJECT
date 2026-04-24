package ru.company.izhs_planner.premium

import android.app.Activity
import android.content.Context
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.Flow

interface PremiumManager {
    val isPremium: Flow<Boolean>
    val dailyLimit: Int get() = 50
    val maxProjectsFree: Int get() = 1
    
    suspend fun checkPremiumStatus()
    suspend fun purchasePremium(activity: Activity)
    suspend fun restorePurchases(activity: Activity)
    fun canUseAI(isPremium: Boolean, messagesToday: Int): Boolean
    fun canCreateProject(isPremium: Boolean, projectCount: Int): Boolean
    fun canExportWithoutWatermark(isPremium: Boolean): Boolean
}

class PremiumManagerImpl(
    private val context: Context,
    private val billingHelper: BillingHelper
) : PremiumManager {
    
    private val prefs = context.getSharedPreferences("premium_prefs", Context.MODE_PRIVATE)
    
    override val isPremium: Flow<Boolean> = kotlinx.coroutines.flow.flow {
        emit(isPremiumActive())
    }
    
    fun isPremiumActive(): Boolean {
        return prefs.getBoolean(KEY_PREMIUM_ACTIVE, false)
    }
    
    override suspend fun checkPremiumStatus() {
        val legacyActive = prefs.getBoolean(KEY_PREMIUM_ACTIVE, false)
        
        if (!legacyActive) {
            val purchased = billingHelper.queryPurchases()
            if (purchased) {
                setPremiumActive(true)
            }
        }
    }
    
    override suspend fun purchasePremium(activity: Activity) {
        billingHelper.purchasePremium(activity)
    }
    
    override suspend fun restorePurchases(activity: Activity) {
        val restored = billingHelper.restorePurchases(activity)
        if (restored) {
            setPremiumActive(true)
        }
    }
    
    fun setPremiumActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_PREMIUM_ACTIVE, active).apply()
    }
    
    override fun canUseAI(isPremium: Boolean, messagesToday: Int): Boolean {
        return isPremium || messagesToday < dailyLimit
    }
    
    override fun canCreateProject(isPremium: Boolean, projectCount: Int): Boolean {
        return isPremium || projectCount < maxProjectsFree
    }
    
    override fun canExportWithoutWatermark(isPremium: Boolean): Boolean {
        return isPremium
    }
    
    companion object {
        private const val KEY_PREMIUM_ACTIVE = "premium_active"
    }
}

interface BillingHelper {
    suspend fun purchasePremium(activity: Activity)
    suspend fun restorePurchases(activity: Activity): Boolean
    suspend fun queryPurchases(): Boolean
}

class RuStoreBillingHelper(private val context: Context) : BillingHelper {
    private val productId = "premium_full"
    private val appKey = "ru.company.izhs_planner"
    
    override suspend fun purchasePremium(activity: Activity) {
        try {
            val iapService = com.android.installreferrer.InstallReferrerClient.newBuilder(context)
            iapService.buildStartConnection(object : com.android.installreferrer.InstallReferrerClient.InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {}
                override fun onInstallReferrerServiceDisconnected() {}
            })
            
            activity.runOnUiThread {
                showRuStorePayment(activity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun showRuStorePayment(activity: Activity) {
        try {
            val intent = android.content.Intent("ru.rustore.iap.BUY")
            intent.setPackage("ru.rustore.store")
            intent.putExtra("product_id", productId)
            intent.putExtra("app_id", appKey)
            activity.startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                context,
                "Ошибка открытия RuStore",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    override suspend fun restorePurchases(activity: Activity): Boolean {
        return false
    }
    
    override suspend fun queryPurchases(): Boolean {
        return false
    }
}

data class PremiumFeature(
    val id: String,
    val name: String,
    val description: String,
    val isUnlocked: Boolean
)

enum class UnlockableFeature(val featureId: String, val displayName: String) {
    UNLIMITED_AI("unlimited_ai", "Безлимитный ИИ"),
    ALL_TEMPLATES("all_templates", "Все шаблоны"),
    EXPORT_WITHOUT_WATERMARK("no_watermark", "Экспорт без водяного знака"),
    PRIORITY_support("priority_support", "Приоритетная поддержка")
}