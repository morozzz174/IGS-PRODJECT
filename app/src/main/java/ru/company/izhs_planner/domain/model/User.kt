package ru.company.izhs_planner.domain.model

data class User(
    val id: String = "",
    val email: String = "",
    val phone: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val isPremium: Boolean = false,
    val premiumExpiryDate: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
) {
    val isLoggedIn: Boolean get() = id.isNotEmpty()
    
    val authMethod: String get() = when {
        phone.isNotEmpty() -> "phone"
        email.isNotEmpty() -> "email"
        else -> "none"
    }
}

data class AuthResult(
    val success: Boolean,
    val user: User? = null,
    val errorMessage: String? = null,
    val verificationId: String? = null
)

sealed class AuthState {
    object NotAuthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}