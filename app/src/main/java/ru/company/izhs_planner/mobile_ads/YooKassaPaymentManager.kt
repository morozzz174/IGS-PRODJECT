package ru.company.izhs_planner.mobile_ads

import android.app.Activity
import android.content.Context
import ru.yoomoney.sdk.kassa.payments.YooKassaPaymentHost
import ru.yoomoney.sdk.kassa.payments.PaymentParameters
import ru.yoomoney.sdk.kassa.payments.TokenizationParameters
import ru.yoomoney.sdk.kassa.payments.customize.CustomizeParameters
import ru.yoomoney.sdk.kassa.payments.payment.SafePaymentActivity
import ru.yoomoney.sdk.kassa.payments.payment.model.PaymentResult
import ru.yoomoney.sdk.kassa.payments.paymentmodel.PaymentMethodType
import ru.yoomoney.sdk.kassa.payments.uikit.view.LibraryStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal
import java.math.RoundingMode

class YooKassaPaymentManager(private val context: Context) {
    private val _paymentState = MutableStateFlow(YooPaymentState.IDLE)
    val paymentState: StateFlow<YooPaymentState> = _paymentState
    
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError
    
    private var shopId: String = ""
    private var secretKey: String = ""
    
    companion object {
        const val PREMIUM_PRICE = 399_00L // 399.00 руб в копейках
    }
    
    fun initialize(shopId: String, secretKey: String) {
        this.shopId = shopId
        this.secretKey = secretKey
        
        try {
            YooKassaPaymentHost.initialize(
                context,
                shopId,
                secretKey,
                "ru.company.izhs_planner"
            )
        } catch (e: Exception) {
            _lastError.value = "Ошибка инициализации: ${e.message}"
        }
    }
    
    fun startPayment(
        activity: Activity,
        title: String = "Премиум ИЖС-Проектировщик",
        description: String = "Безлимитный ИИ и расширенные функции",
        customProperties: Map<String, Any>? = null,
        onComplete: (Boolean, String?) -> Unit
    ) {
        _paymentState.value = YooPaymentState.PROCESSING
        _lastError.value = null
        
        val amount = BigDecimal(PREMIUM_PRICE)
            .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        
        val paymentParams = PaymentParameters.Builder()
            .setTitle(title)
            .setDescription(description)
            .setAmount(amount, "RUB")
            .setReturnUrl("")
            .setCustomProperties(customProperties?.toMapString() ?: emptyMap())
            .build()
        
        val tokenizationParams = TokenizationParameters.Builder()
            .setPaymentMethodTypes(
                listOf(PaymentMethodType.BANK_CARD, PaymentMethodType.SBP)
            )
            .setShowScanQr(false)
            .build()
        
        val customizeParams = CustomizeParameters.Builder()
            .setCustomBackgroundColor("")
            .setCustomButtonColor("")
            .setCustomExitOnSaveCard(false)
            .build()
        
        try {
            SafePaymentActivity.createPaymentIntent(
                parameters = paymentParams,
                tokenizationParameters = tokenizationParams,
                customizeParameters = customizeParams
            ).observe { result ->
                when (result) {
                    is PaymentResult.Success -> {
                        _paymentState.value = YooPaymentState.SUCCESS
                        println("Payment token: ${result.token}")
                        onComplete(true, result.token)
                    }
                    is PaymentResult.Error -> {
                        _paymentState.value = YooPaymentState.ERROR
                        _lastError.value = result.errorMessage
                        onComplete(false, result.errorMessage)
                    }
                    is PaymentResult.Cancelled -> {
                        _paymentState.value = YooPaymentState.CANCELLED
                        onComplete(false, "Платёж отменён")
                    }
                }
            }
        } catch (e: Exception) {
            _paymentState.value = YooPaymentState.ERROR
            _lastError.value = e.message
            onComplete(false, e.message)
        }
    }
    
    fun getTestPayment() {
        // YooKassa provides test mode - no special setup needed
        // All payments in test environment use test cards
    }
    
    fun cancelPayment() {
        _paymentState.value = YooPaymentState.CANCELLED
    }
    
    fun resetState() {
        _paymentState.value = YooPaymentState.IDLE
        _lastError.value = null
    }
    
    private fun Map<String, Any>.toMapString(): Map<String, String> {
        return this.mapValues { it.value.toString() }
    }
}

enum class YooPaymentState {
    IDLE,
    PROCESSING,
    SUCCESS,
    ERROR,
    CANCELLED
}