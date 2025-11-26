package com.example.ratify.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ratify.handlers.AuthHandler
import com.example.ratify.viewmodels.AuthViewModel
import com.example.ratify.viewmodels.AuthViewModelFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.graphics.SolidColor
import com.example.ratify.handlers.FirestoreApiHandler
private const val WEB_CLIENT_ID = "231048600556-88sij8v4769v8k7eraoodtpkltajpgl9.apps.googleusercontent.com"
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    authHandler: AuthHandler,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val firestoreHandler = remember { FirestoreApiHandler() }
    val viewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(authHandler,firestoreHandler)
    )
    val uiState by viewModel.uiState.collectAsState()
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()
    }

    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken

            if (idToken != null) {
                viewModel.signInWithGoogle(idToken, onSuccess = onAuthSuccess)
            } else {
                Toast.makeText(context, "Error: Token de Google no encontrado.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Toast.makeText(context, "Google Sign-In falló: ${e.statusCode}", Toast.LENGTH_LONG).show()
        }
    }
    Scaffold(containerColor = BackgroundColor) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (uiState.isLoginMode) "Iniciar Sesión" else "Crear Cuenta",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::setEmail,
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = getAuthTextFieldColors()
            )
            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::setPassword,
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                colors = getAuthTextFieldColors()
            )

            Button(
                onClick = { viewModel.authenticate(onAuthSuccess) },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        if (uiState.isLoginMode) "INGRESAR" else "REGISTRARME",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Divider(
                color = Color.Gray.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                thickness = 1.dp
            )

            OutlinedButton(
                onClick = {
                    val signInIntent = googleSignInClient.signInIntent
                    googleLauncher.launch(signInIntent)
                },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                    containerColor = Color.Transparent
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(Color.Gray))
            ) {
                Text("Iniciar Sesión con Google", color = Color.White)
            }
            uiState.error?.let {
                Text(
                    text = "Error: $it",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            TextButton(onClick = viewModel::toggleMode) {
                Text(
                    text = if (uiState.isLoginMode) "¿No tienes cuenta? Regístrate." else "¿Ya tienes cuenta? Inicia Sesión.",
                    color = PrimaryColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun getAuthTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = CardColor,
    unfocusedContainerColor = CardColor,
    disabledContainerColor = CardColor,
    focusedBorderColor = PrimaryColor,
    unfocusedBorderColor = Color.Gray,
    focusedLabelColor = PrimaryColor,
    unfocusedLabelColor = Color.Gray,
    cursorColor = PrimaryColor,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
)