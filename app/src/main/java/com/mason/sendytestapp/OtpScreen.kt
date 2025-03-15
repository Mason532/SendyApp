package com.mason.sendytestapp

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreInterceptKeyBeforeSoftKeyboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun Otp(
    modifier: Modifier = Modifier,
    phone: String,
    otpResendStatus: SharedFlow<OtpSendStatus>,
    otpConfirmStatus: SharedFlow<OtpConfirmStatus>,
    onSucceedOtp: () -> Unit,
    onContinueClicked: (String) -> Unit,
    onCodeResend: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    var otp by rememberSaveable { mutableStateOf("") }
    var otpState by remember { mutableStateOf(TextFieldValue(otp)) }

    var isValid by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }

    fun isValidOtp(input: String): Boolean {
        return input.matches("^\\d{6}$".toRegex())
    }
    
    var countdown by rememberSaveable {  mutableIntStateOf(60) }
    var isCodeSending by rememberSaveable { mutableStateOf(false) }

    var isCodeConfirmedSuccessfuly by rememberSaveable { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose {
            val currentState = lifecycleOwner.lifecycle.currentState
            if (currentState != Lifecycle.State.DESTROYED) {
                if (isCodeConfirmedSuccessfuly) {
                    isCodeConfirmedSuccessfuly = false
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(isCodeSending) {
        if (isCodeSending) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                otpResendStatus.collect {
                    when {
                        it.isSendSucceed -> {
                            //isCodeSending = false
                            Toast.makeText(
                                context,
                                "Код упешно отправлен!",
                                Toast.LENGTH_SHORT
                            ).show()
                            countdown = 60
                        }

                        it.isSendSucceed == false ->
                            Toast.makeText(context,
                                "Не удалось отправить😞 Попробуйте ещё раз!",
                                Toast.LENGTH_SHORT
                            ).show()

                        it.sendingError != null -> {
                            Toast.makeText(
                                context,
                                "Произошла ошибка😞 Попробуйте ещё раз!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    isCodeSending = false
                }
            }
        } else {
            while (countdown > 0) {
                delay(1_000L)
                countdown--
            }
        }
    }

    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            otpConfirmStatus.collect {
                if (isLoading) {
                    val isOtpConfirmed = it.otpConfirmResult.first
                    val restOtpConfirmTry = it.otpConfirmResult.second
                    when {
                        isOtpConfirmed -> {
                            withContext(Dispatchers.Main.immediate) {
                                isCodeConfirmedSuccessfuly = true
                                onSucceedOtp()
                            }
                        }

                        isOtpConfirmed == false -> {
                            if (restOtpConfirmTry > 0)
                                Toast.makeText(context,
                                    "Неверный код активации, у вас осталось $restOtpConfirmTry попыток",
                                    Toast.LENGTH_SHORT
                                ).show()
                            else
                                Toast.makeText(
                                    context,
                                    "Запросите новый код активации",
                                    Toast.LENGTH_SHORT
                                ).show()

                        }

                        it.otpConfirmError != null -> {
                            Toast.makeText(
                                context,
                                "Произошла ошибка, попробуйте ещё раз 😞",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    isLoading = false
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

        Spacer(modifier = Modifier.height(16.dp))

        if (countdown > 0) {
            Text(
                text = "Отправить новый код можно через $countdown сек.",
                color = Color.Gray,
                fontSize = 14.sp
            )
        } else {
            Text(
                text = "Отправить новый код",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    if (!isCodeSending) {
                        isCodeSending = true
                        onCodeResend(phone)
                    }
                }
            )
        }
    }
}