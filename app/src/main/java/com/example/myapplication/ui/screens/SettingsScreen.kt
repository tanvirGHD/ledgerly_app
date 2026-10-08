package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.viewmodel.AuthViewModel

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    val userName by authViewModel.userName.collectAsState()
    val userEmail by authViewModel.userEmail.collectAsState()
    val currency by authViewModel.currency.collectAsState()
    val isDarkMode by authViewModel.isDarkMode.collectAsState()

    var nameInput by remember { mutableStateOf(userName) }
    var currencyInput by remember { mutableStateOf(currency) }
    var profileMessage by remember { mutableStateOf<String?>(null) }

    var newPasswordInput by remember { mutableStateOf("") }
    var passwordMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Account Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Logged in as: $userEmail", style = MaterialTheme.typography.bodySmall)

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = currencyInput,
                    onValueChange = { currencyInput = it },
                    label = { Text("Currency (e.g. BDT, USD)") },
                    modifier = Modifier.fillMaxWidth()
                )

                profileMessage?.let { msg ->
                    Text(msg, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        authViewModel.updateProfile(
                            name = nameInput,
                            currency = currencyInput,
                            onSuccess = { profileMessage = "Profile updated successfully!" },
                            onError = { profileMessage = it }
                        )
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save Profile")
                }
            }
        }

        // Dark Mode Preference
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Dark Mode Theme", fontWeight = FontWeight.Bold)
                    Text("Enable deep navy dark theme", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { authViewModel.toggleDarkMode(it) }
                )
            }
        }

        // Password Change Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Change Password", fontWeight = FontWeight.Bold, fontSize = 18.sp)

                OutlinedTextField(
                    value = newPasswordInput,
                    onValueChange = { newPasswordInput = it },
                    label = { Text("New Password (min 6 chars)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                passwordMessage?.let { msg ->
                    Text(msg, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        if (newPasswordInput.length < 6) {
                            passwordMessage = "Password must be at least 6 characters."
                            return@Button
                        }
                        authViewModel.updatePassword(
                            newPassword = newPasswordInput,
                            onSuccess = {
                                newPasswordInput = ""
                                passwordMessage = "Password updated successfully!"
                            },
                            onError = { passwordMessage = it }
                        )
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Update Password")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Logout Button
        Button(
            onClick = {
                authViewModel.logout()
                onLogout()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Log Out", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
