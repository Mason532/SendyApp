package com.mason.sendytestapp

import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreInterceptKeyBeforeSoftKeyboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext

object PhoneNumberConst {
    const val RU_PHONE_CODE = "+7"
    const val PHONE_NUMBER_LENGTH = 12
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    otpSendStatus: SharedFlow<OtpSendStatus>,
    onContinueClicked: (String) -> Unit,
    onCodeSent: (String) -> Unit,
    onTermOfUseShow: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val lifecycleOwner = LocalLifecycleOwner.current

    val phoneCodeLength = remember {PhoneNumberConst.RU_PHONE_CODE.length}
    val phoneNumber = rememberSaveable { mutableStateOf(PhoneNumberConst.RU_PHONE_CODE) }
    var phoneNumberTextFieldValue by remember {
        mutableStateOf(TextFieldValue(phoneNumber.value, TextRange(phoneNumber.value.length)))
    }

    var isValid by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var isNavigationCompleted by remember { mutableStateOf(false) }
    var isAgreementChecked by rememberSaveable { mutableStateOf(false) }

    val phoneNumberRegex = "^\\+7\\d{10}$".toRegex()
    fun validatePhoneNumber(phone: String): Boolean {
        return phoneNumberRegex.matches(phone)
    }

    DisposableEffect(Unit) {
        onDispose {
            val currentState = lifecycleOwner.lifecycle.currentState
            if (currentState != Lifecycle.State.DESTROYED) {
                if (isNavigationCompleted) {
                    isNavigationCompleted = false
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            otpSendStatus.collect{
                if (isLoading) {
                    when {
                        it.isSendSucceed -> {
                            withContext(Dispatchers.Main.immediate) {
                                isNavigationCompleted = true
                                onCodeSent(phoneNumber.value)
                            }
                        }

                        it.sendingError != null -> {
                            Toast.makeText(
                                context,
                                "Произошла ошибка.😞 Попробуйте ещё раз!",
                                Toast.LENGTH_SHORT
                            ).show()
                            isLoading = false
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Введите номер телефона на который мы вышлем проверочный код для входа в приложение",
            style = MaterialTheme.typography.bodyLarge,
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            value = phoneNumberTextFieldValue,
            onValueChange = { newValue ->
                if (newValue.text.startsWith(PhoneNumberConst.RU_PHONE_CODE)) {
                    val filteredValue = newValue.text.filterIndexed { index, char ->
                        index < phoneCodeLength || char.isDigit()
                    }

                    if (filteredValue.length <= PhoneNumberConst.PHONE_NUMBER_LENGTH) {
                        phoneNumber.value = filteredValue
                        phoneNumberTextFieldValue = newValue.copy(text = filteredValue)
                        isValid = validatePhoneNumber(phoneNumber.value)
                    }
                }
            },
            label = { Text("Номер телефона") },
            placeholder = { Text("+7 XXX XXX XX XX") },
            isError = !isValid && isFocused,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
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
                },
            interactionSource = interactionSource,
            supportingText = {
                if (!isValid && isFocused) {
                    Text(
                        text = "Номер телефона введен некорректно.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = if (isValid) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = if (isValid || !isFocused) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                focusedLabelColor = if (isValid) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                unfocusedLabelColor = if (isValid || !isFocused) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
            )
        )

        Row(
            modifier = modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isAgreementChecked,
                onCheckedChange = { isAgreementChecked = it }
            )

            val annotatedText = buildAnnotatedString {
                val start = length
                append("Я принимаю условия оферты")
                val end = length

                addStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    ),
                    start = start,
                    end = end
                )
            }

            Text(
                text = annotatedText,
                modifier = modifier.clickable {
                    onTermOfUseShow()
                    isAgreementChecked = true
                }
            )
        }

        Button(
            onClick = {
                if (isValid) {
                    isLoading = true
                    onContinueClicked(phoneNumber.value)
                }
            },
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            enabled = isValid && !isLoading && isAgreementChecked
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Продолжить")
            }
        }
    }
}