package com.mason.sendytestapp

import android.widget.Toast
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreInterceptKeyBeforeSoftKeyboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun Otp(
    modifier: Modifier = Modifier,
    navController: NavController,
    phone: String,
    otpScreenState: State<OtpScreenState>,
    onContinueClicked: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    var otp by rememberSaveable { mutableStateOf("") }
    var isValid by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    fun isValidOtp(input: String): Boolean {
        return input.matches("^\\d{6}$".toRegex())
    }

    LaunchedEffect(otpScreenState.value) {
        val state = otpScreenState.value
        if (state.isOtpConfirmed) {
            navController.navigate(AppNavigationDestinations.finalScreen) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
        if (state.otpConfirmError != null) {
            Toast.makeText(context, "Произошла ошибка, попробуйте ещё раз 😞", Toast.LENGTH_SHORT).show()
        }

        isLoading = false
    }

    var otpState by remember { mutableStateOf(TextFieldValue(otp)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Введите код из 6 цифр, отправленный на номер $phone",
            style = MaterialTheme.typography.bodyLarge,
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            value = otpState,
            onValueChange = { newValue ->
                if (newValue.text.length <= 6 && newValue.text.all { it.isDigit() }) {
                    otpState = newValue
                    otp = newValue.text
                    isValid = isValidOtp(otp)
                }
            },
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            ),
            label = { Text("Код") },
            interactionSource = interactionSource,
            placeholder = { Text("******") },
            isError = !isValid && isFocused,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            singleLine = true,
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .onPreInterceptKeyBeforeSoftKeyboard {
                    if (it.key == Key.Back) {
                        focusManager.clearFocus()
                    }
                    false
                }
            ,
            supportingText = {
                if (!isValid && isFocused) {
                    Text(
                        text = "Код должен состоять из 6 цифр",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                unfocusedBorderColor = if (isValid || !isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                focusedLabelColor = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                unfocusedLabelColor = if (isValid || !isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        )

        Button(
            onClick = {
                if (isValid) {
                    isLoading = true
                    onContinueClicked(otp)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
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