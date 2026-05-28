package ru.company.izhs_planner.mobile_ads

import android.app.Activity
import android.content.Context
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
        const val PREMIUM_PRICE = 399_00L
    }

    fun initialize(shopId: String, secretKey: String) {
        this.shopId = shopId
        this.secretKey = secretKey
    }

    fun startPayment(
        activity: Activity,
        title: String = "Премиум ИЖС-Проектировщик",
        description: String = "Безлимитный ИИ и расширенные функции",
        customProperties: Map<String, Any>? = null,
        onComplete: (Boolean, String?) -> Unit
    ) {
        _paymentState.value = YooPaymentState.SUCCESS
        onComplete(true, null)
    }

    fun getTestPayment() {
    }

    fun cancelPayment() {
        _paymentState.value = YooPaymentState.CANCELLED
    }

    fun resetState() {
        _paymentState.value = YooPaymentState.IDLE
        _lastError.value = null
    }
}

enum class YooPaymentState {
    IDLE,
    PROCESSING,
    SUCCESS,
    ERROR,
    CANCELLED
}
