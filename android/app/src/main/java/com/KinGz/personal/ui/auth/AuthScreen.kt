package com.KinGz.personal.ui.auth
import com.KinGz.personal.network.UserResponse
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.KinGz.personal.network.AuthRepository
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthenticated: (UserResponse) -> Unit
) {
    val context = LocalContext.current
    val repository = remember {
        AuthRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()

    var isRegistering by remember { mutableStateOf(false) }
    var isAdminEnrollment by remember { mutableStateOf(false) }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var enrollmentSecret by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "KinGz",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = when {
                isAdminEnrollment -> "Authorize this admin device"
                isRegistering -> "Create your account"
                else -> "Sign in to your personal hub"
            },
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                errorMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Username") },
            singleLine = true,
            enabled = !isLoading
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            enabled = !isLoading
        )

        if (isAdminEnrollment) {
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = enrollmentSecret,
                onValueChange = {
                    enrollmentSecret = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Admin enrollment secret") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                enabled = !isLoading
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Use this only to authorize your own admin phone.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (isRegistering && !isAdminEnrollment) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Username: 3–50 characters. Password: 8–128 characters.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        errorMessage?.let { message ->
            Spacer(Modifier.height(12.dp))

            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                val cleanUsername = username.trim()

                if (cleanUsername.length !in 3..50) {
                    errorMessage = "Username must be 3–50 characters."
                    return@Button
                }

                if (password.length !in 8..128) {
                    errorMessage = "Password must be 8–128 characters."
                    return@Button
                }

                if (isAdminEnrollment && enrollmentSecret.isBlank()) {
                    errorMessage = "Enter the admin enrollment secret."
                    return@Button
                }

                scope.launch {
                    isLoading = true
                    errorMessage = null

                    try {
                        val result = when {
                            isAdminEnrollment -> {
                                repository.enrollAdminDevice(
                                    username = cleanUsername,
                                    password = password,
                                    enrollmentSecret = enrollmentSecret
                                )
                            }

                            isRegistering -> {
                                repository.register(
                                    cleanUsername,
                                    password
                                )
                            }

                            else -> {
                                repository.login(
                                    cleanUsername,
                                    password
                                )
                            }
                        }

                        result.fold(
                            onSuccess = { user ->
                                enrollmentSecret = ""
                                onAuthenticated(user)
                            },
                            onFailure = { error ->
                                errorMessage =
                                    error.message ?: "Authentication failed."
                            }
                        )
                    } catch (error: Exception) {
                        errorMessage =
                            error.message ?: "Something went wrong."
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    when {
                        isAdminEnrollment -> "Authorize & Log In"
                        isRegistering -> "Create Account"
                        else -> "Log In"
                    }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = {
                isAdminEnrollment = !isAdminEnrollment
                isRegistering = false
                enrollmentSecret = ""
                errorMessage = null
            },
            enabled = !isLoading
        ) {
            Text(
                if (isAdminEnrollment) {
                    "Back to normal login"
                } else {
                    "Enroll admin device"
                }
            )
        }

        TextButton(
            onClick = {
                isRegistering = !isRegistering
                isAdminEnrollment = false
                enrollmentSecret = ""
                errorMessage = null
            },
            enabled = !isLoading
        ) {
            Text(
                if (isRegistering) {
                    "Already have an account? Log in"
                } else {
                    "New to KinGz? Create an account"
                }
            )
        }
    }
}
