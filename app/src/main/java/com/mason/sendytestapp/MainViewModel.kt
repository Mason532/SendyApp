package com.mason.sendytestapp

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import land.sendy.pfe_sdk.api.API

data class LoginScreenState(
    var sendingError: Throwable? = null,
    var isSendSucceed: Boolean = false
)

data class OtpScreenState(
    var otpConfirmError: Throwable? = null,
    var isOtpConfirmed: Boolean = false
)

class MainViewModel: ViewModel() {
    /*
    companion object {
        const val SPLASH_SCREEN_DURATION = 3000L
    }

    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()
     */

    private val sendyOtpSendCallback: OtpCallback by lazy {
        SendyCallback()
    }

    private val sendyOtpCheckCallback: OtpCallback by lazy {
        SendyCallback()
    }

    var loginScreenState = mutableStateOf(
        LoginScreenState()
    )
        private set

    var otpScreenState = mutableStateOf(
        OtpScreenState()
    )
        private set

    init {
        /*
        viewModelScope.launch {
            delay(SPLASH_SCREEN_DURATION)
            _isReady.value = true
        }
         */
    }

    fun sendCode(api: API, phoneNumber: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            //если указать новые Job в каждой из дочерних корутин, то мы сломаем Structured concurrency
            launch {
                sendyOtpSendCallback.isSendSucceed.collect {
                    loginScreenState.value = loginScreenState.value.copy(isSendSucceed = it)
                    coroutineContext.cancelChildren()
                }
            }

            launch {
                sendyOtpSendCallback.sendingError.collect {
                    loginScreenState.value = loginScreenState.value.copy(sendingError = it)
                    coroutineContext.cancelChildren()
                }
            }
        }

        api.loginAtAuth(context, phoneNumber, sendyOtpSendCallback)

    }

    fun checkOtp(context: Context, api: API, otp: String) {
        viewModelScope.launch(Dispatchers.IO) {
            launch {
                sendyOtpCheckCallback.isSendSucceed.collect {
                    otpScreenState.value = otpScreenState.value.copy(isOtpConfirmed = it)
                    coroutineContext.cancelChildren()
                }
            }

            launch {
                sendyOtpCheckCallback.sendingError.collect {
                    otpScreenState.value = otpScreenState.value.copy(otpConfirmError = it)
                    coroutineContext.cancelChildren()
                }
            }
        }

        api.activateWllet(context, otp, "sms", sendyOtpCheckCallback)
    }
}