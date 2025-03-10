package com.mason.sendytestapp

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    loginScreenState: State<LoginScreenState>,
    onContinueClicked: (String) -> Unit
) {
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var isValid by rememberSaveable { mutableStateOf(true) }
    var isLoading by rememberSaveable { mutableStateOf(false) }

    val phoneNumberRegex = "^\\+7\\d{10}$".toRegex()
    fun validatePhoneNumber(phone: String): Boolean {
        return phoneNumberRegex.matches(phone)
    }

    val context = LocalContext.current

    LaunchedEffect(loginScreenState.value) {
        val state = loginScreenState.value

        if (state.isSendSucceed) {
            navController.navigate(AppNavigationDestinations.otpScreen)
        }

        if (state.sendingError != null) {
            Toast.makeText(context, "Произошла ошибка, попробуйте ещё раз 😞", Toast.LENGTH_SHORT).show()
        }

        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Введите номер телефона на который мы вышлем проверочный код для входа в приложение",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { newValue ->
                val newValueWithoutSpaces = newValue.replace(" ", "")
                if (newValueWithoutSpaces != phoneNumber) {
                    phoneNumber = newValueWithoutSpaces
                    isValid = validatePhoneNumber(newValue)
                }
            },
            label = { Text("Номер телефона") },
            placeholder = { Text("+7 XXX XXX XX XX") },
            isError = !isValid,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            supportingText = {
                if (!isValid) {
                    Text(
                        text = "Номер телефона введен некорректно.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                unfocusedBorderColor = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                focusedLabelColor = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                unfocusedLabelColor = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        )

        Button(
            onClick = {
                if (phoneNumber.isNotEmpty() && isValid) {
                    isLoading = true
                    onContinueClicked(phoneNumber)
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            enabled = isValid && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Продолжить")
            }
        }

    }
}