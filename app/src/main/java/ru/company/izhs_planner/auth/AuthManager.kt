package ru.company.izhs_planner.auth

import android.app.Activity
import android.content.Context
import androidx.annotation.Keep
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.company.izhs_planner.domain.model.AuthResult
import ru.company.izhs_planner.domain.model.AuthState
import ru.company.izhs_planner.domain.model.User
import java.util.concurrent.TimeUnit

class AuthManager(private val context: Context) {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.NotAuthenticated)
    val authState: StateFlow<AuthState> = _authState
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser
    
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    
    init {
        checkCurrentUser()
    }
    
    private fun checkCurrentUser() {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            _authState.value = AuthState.Loading
            GlobalScope.launch {
                loadUserFromFirestore(firebaseUser.uid)
            }
        } else {
            _authState.value = AuthState.NotAuthenticated
            _currentUser.value = null
        }
    }
    
    suspend fun initialize(): Boolean {
        return try {
            FirebaseApp.initializeApp(context)
            auth.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (user != null) {
                    _authState.value = AuthState.Authenticated(User(id = user.uid))
                } else {
                    _authState.value = AuthState.NotAuthenticated
                    _currentUser.value = null
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun isLoggedIn(): Boolean = auth.currentUser != null
    
    fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    // ═══════════════════════════════════════════════════════════
    // EMAIL AUTHENTICATION
    // ═══════════════════════════════════════════════════════════
    
    suspend fun sendPasswordResetEmail(email: String): AuthResult {
        return try {
            auth.sendPasswordResetEmail(email).await()
            AuthResult(success = true, user = null)
        } catch (e: Exception) {
            AuthResult(success = false, errorMessage = getErrorMessage(e))
        }
    }
    
    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        _authState.value = AuthState.Loading
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                val user = User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString() ?: ""
                )
                updateLastLogin(firebaseUser.uid)
                saveUserToFirestore(user)
                _currentUser.value = user
                _authState.value = AuthState.Authenticated(user)
                AuthResult(success = true, user = user)
            } else {
                _authState.value = AuthState.NotAuthenticated
                AuthResult(success = false, errorMessage = "Ошибка входа")
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(getErrorMessage(e))
            AuthResult(success = false, errorMessage = getErrorMessage(e))
        }
    }
    
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): AuthResult {
        _authState.value = AuthState.Loading
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                firebaseUser.updateProfile(
                    com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                ).await()
                
                val user = User(
                    id = firebaseUser.uid,
                    email = email,
                    displayName = displayName
                )
                saveUserToFirestore(user)
                _currentUser.value = user
                _authState.value = AuthState.Authenticated(user)
                AuthResult(success = true, user = user)
            } else {
                _authState.value = AuthState.NotAuthenticated
                AuthResult(success = false, errorMessage = "Ошибка регистрации")
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(getErrorMessage(e))
            AuthResult(success = false, errorMessage = getErrorMessage(e))
        }
    }
    
    // ═══════════════════════════════════════════════════════════
    // PHONE AUTHENTICATION
    // ═══════════════════════════════════════════════════════════
    
    fun startPhoneVerification(
        phoneNumber: String,
        onCodeSent: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60, TimeUnit.SECONDS)
            .setActivity(context as Activity)
            .build()
        
        PhoneAuthProvider.verifyPhoneNumber(options)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    verificationId = task.result?.verificationId
                    onCodeSent(verificationId!!)
                } else {
                    onError(getErrorMessage(task.exception))
                }
            }
    }
    
    suspend fun verifyPhoneCode(code: String): AuthResult {
        val vid = verificationId
        if (vid == null) {
            return AuthResult(success = false, errorMessage = "Код не отправлен")
        }
        
        _authState.value = AuthState.Loading
        return try {
            val credential = PhoneAuthProvider.getCredential(vid, code)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                val phoneNumber = firebaseUser.phoneNumber ?: ""
                val user = User(
                    id = firebaseUser.uid,
                    phone = phoneNumber,
                    displayName = firebaseUser.displayName ?: "Пользователь"
                )
                saveUserToFirestore(user)
                updateLastLogin(firebaseUser.uid)
                _currentUser.value = user
                _authState.value = AuthState.Authenticated(user)
                AuthResult(success = true, user = user)
            } else {
                _authState.value = AuthState.NotAuthenticated
                AuthResult(success = false, errorMessage = "Ошибка верификации")
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(getErrorMessage(e))
            AuthResult(success = false, errorMessage = getErrorMessage(e))
        }
    }
    
    // ═══════════════════════════════════════════════════════════
    // CLOUD STORAGE (FIRESTORE)
    // ═══════════════════════════════════════════════════════════
    
    private suspend fun saveUserToFirestore(user: User) {
        try {
            firestore.collection("users")
                .document(user.id)
                .set(mapOf(
                    "id" to user.id,
                    "email" to user.email,
                    "phone" to user.phone,
                    "displayName" to user.displayName,
                    "isPremium" to user.isPremium,
                    "premiumExpiryDate" to user.premiumExpiryDate,
                    "createdAt" to user.createdAt,
                    "lastLoginAt" to user.lastLoginAt
                )).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private suspend fun loadUserFromFirestore(uid: String) {
        try {
            val doc = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            
            if (doc.exists()) {
                val user = User(
                    id = doc.getString("id") ?: uid,
                    email = doc.getString("email") ?: "",
                    phone = doc.getString("phone") ?: "",
                    displayName = doc.getString("displayName") ?: "",
                    isPremium = doc.getBoolean("isPremium") ?: false,
                    premiumExpiryDate = doc.getLong("premiumExpiryDate") ?: 0L,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
                _currentUser.value = user
                _authState.value = AuthState.Authenticated(user)
            } else {
                val firebaseUser = auth.currentUser
                if (firebaseUser != null) {
                    val user = User(
                        id = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        displayName = firebaseUser.displayName ?: ""
                    )
                    saveUserToFirestore(user)
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated(user)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private suspend fun updateLastLogin(uid: String) {
        try {
            firestore.collection("users")
                .document(uid)
                .update("lastLoginAt", System.currentTimeMillis()).await()
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    // ═══════════════════════════════════════════════════════════
    // PREMIUM STATUS
    // ═══════════════════════════════════════════════════════════
    
    suspend fun updatePremiumStatus(isPremium: Boolean, expiryDate: Long) {
        val uid = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users")
                .document(uid)
                .update(
                    mapOf(
                        "isPremium" to isPremium,
                        "premiumExpiryDate" to expiryDate
                    )
                ).await()
            
            _currentUser.value?.let { user ->
                _currentUser.value = user.copy(
                    isPremium = isPremium,
                    premiumExpiryDate = expiryDate
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun refreshPremiumStatus(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val doc = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            
            if (doc.exists()) {
                val isPremium = doc.getBoolean("isPremium") ?: false
                val expiryDate = doc.getLong("premiumExpiryDate") ?: 0L
                val now = System.currentTimeMillis()
                
                val premiumActive = isPremium && (expiryDate == 0L || expiryDate > now)
                _currentUser.value?.let { user ->
                    _currentUser.value = user.copy(
                        isPremium = premiumActive,
                        premiumExpiryDate = expiryDate
                    )
                }
                premiumActive
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    // ═══════════════════════════════════════════════════════════
    // LOGOUT
    // ═══════════════════════════════════════════════════════════
    
    fun signOut() {
        auth.signOut()
        _currentUser.value = null
        _authState.value = AuthState.NotAuthenticated
    }
    
    // ═══════════════════════════════════════════════════════════
    // HELPER
    // ═══════════════════════════════════════════════════════════
    
    private fun getErrorMessage(e: Exception?): String {
        return when {
            e?.message?.contains("EMAIL_EXISTS") == true -> "Email уже зарегистрирован"
            e?.message?.contains("INVALID_EMAIL") == true -> "Неверный формат email"
            e?.message?.contains("WEAK_PASSWORD") == true -> "Слишком слабый пароль"
            e?.message?.contains("INVALID_CREDENTIAL") == true -> "Неверные данные для входа"
            e?.message?.contains("INVALID_SESSION_KEY") == true -> "Сессия устарела"
            e?.message?.contains("TOO_MANY_ATTEMPTS") == true -> "Слишком много попыток"
            e?.message?.contains("USER_DISABLED") == true -> "Аккаунт заблокирован"
            e?.message?.contains("USER_NOT_FOUND") == true -> "Пользователь не найден"
            e?.message?.contains("NETWORK_ERROR") == true -> "Ошибка сети"
            else -> e?.message ?: "Произошла ошибка"
        }
    }
    
    companion object {
        const val FIREBASE_API_KEY = "YOUR_API_KEY"
        const val FIREBASE_AUTH_DOMAIN = "your-project.firebaseapp.com"
        const val FIREBASE_DB_URL = "https://your-project.firebaseio.com"
        const val FIREBASE_PROJECT_ID = "your-project"
    }
}