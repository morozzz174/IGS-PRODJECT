package ru.company.izhs_planner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onEmailSignIn: (email: String, password: String) -> Unit,
    onEmailSignUp: (email: String, password: String, name: String) -> Unit,
    onPhoneSignIn: (phone: String) -> Unit,
    onSkip: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var isSignUp by remember { mutableStateOf(false) }
    var isPhoneMode by remember { mutableStateOf(false) }
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var showVerification by remember { mutableStateOf(false) }
    
    var passwordVisible by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "ИЖС-Проектировщик",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (isSignUp) "Создайте аккаунт" else "Войдите в аккаунт",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Toggle: Email / Phone
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !isPhoneMode,
                onClick = { isPhoneMode = false; showVerification = false },
                label = { Text("Email") },
                enabled = !isLoading
            )
            FilterChip(
                selected = isPhoneMode,
                onClick = { isPhoneMode = true; showVerification = false },
                label = { Text("Телефон") },
                enabled = !isLoading
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Error message
        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        if (isPhoneMode && !showVerification) {
            // Phone input
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Номер телефона") },
                placeholder = { Text("+7 999 123-45-67") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onPhoneSignIn(phoneNumber)
                    }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { onPhoneSignIn(phoneNumber) },
                modifier = Modifier.fillMaxWidth(),
                enabled = phoneNumber.length > 5 && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Отправить код")
                }
            }
        } else if (isPhoneMode && showVerification) {
            // Verification code
            OutlinedTextField(
                value = verificationCode,
                onValueChange = { verificationCode = it },
                label = { Text("Код из SMS") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { /* Handle verification */ },
                modifier = Modifier.fillMaxWidth(),
                enabled = verificationCode.length >= 6 && !isLoading
            ) {
                Text("Подтвердить")
            }
        } else {
            // Email mode
            if (isSignUp) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Имя") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (isSignUp) {
                            onEmailSignUp(email, password, displayName)
                        } else {
                            onEmailSignIn(email, password)
                        }
                    }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    if (isSignUp) {
                        onEmailSignUp(email, password, displayName)
                    } else {
                        onEmailSignIn(email, password)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotEmpty() && password.length >= 6 && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (isSignUp) "Создать аккаунт" else "Войти")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = { isSignUp = !isSignUp },
                enabled = !isLoading
            ) {
                Text(
                    if (isSignUp) "Уже есть аккаунт? Войти" else "Создать аккаунт"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        TextButton(
            onClick = onSkip,
            enabled = !isLoading
        ) {
            Text("Продолжить без регистрации")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Пропустив регистрацию, вы не сможете синхронизировать проекты между устройствами",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}