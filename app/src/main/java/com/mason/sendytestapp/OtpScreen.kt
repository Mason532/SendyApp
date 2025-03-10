package com.mason.sendytestapp

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Preview(showBackground = true)
@Composable
fun OtpPreview() {
    val state = remember {mutableStateOf(OtpScreenState())}
    val cntrl = rememberNavController()
    Otp(
        Modifier, cntrl, state, {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Otp(
    modifier: Modifier = Modifier,
    navController: NavController,
    otpScreenState: State<OtpScreenState>,
    onContinueClicked: (String) -> Unit
) {
    var otp by rememberSaveable { mutableStateOf("") }
    var isValid by rememberSaveable { mutableStateOf(true) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var isAgreementChecked by rememberSaveable { mutableStateOf(false) }

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Введите код, отправленный на номер мобильного телефона",
            style = MaterialTheme.typography.bodyLarge,
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            value = otp,
            onValueChange = { newValue ->
                val newValueWithoutSpaces = newValue.replace(" ", "")
                if (newValueWithoutSpaces != otp) {
                    otp = newValueWithoutSpaces
                    isValid = isValidOtp(otp)
                }
            },
            label = { Text("Код") },
            placeholder = { Text("******") },
            isError = !isValid,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone
            ),
            singleLine = true,
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            supportingText = {
                if (!isValid) {
                    Text(
                        text = "Код должен состоять из 6 цифр",
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

        Row(
            modifier = Modifier
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
            )
        }


        Button(
            onClick = {
                if (otp.isNotEmpty() && isValid && isAgreementChecked) {
                    isLoading = true
                    onContinueClicked(otp)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            enabled = isValid && isAgreementChecked && !isLoading
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