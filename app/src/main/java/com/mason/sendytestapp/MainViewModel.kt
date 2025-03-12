package com.mason.sendytestapp

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import land.sendy.pfe_sdk.api.API
import java.io.File

data class LoginScreenState(
    var sendingError: Throwable? = null,
    var isSendSucceed: Boolean = false,
)

data class OtpScreenState(
    var otpConfirmError: Throwable? = null,
    var isOtpConfirmed: Boolean = false
)

class MainViewModel: ViewModel() {
    companion object{
        const val USER_AGREEMENT_FILE = "user_agreement.html"
        const val TOKEN_TYPE = "sms"
    }

    private val sendyGetTermOfUseCallback: TermOfUseCallback by  lazy {
        SendyTestAppTermOfUseCallback()
    }

    private val sendyOtpSendCallback: SendOtpCallback by lazy {
        SendyTestAppSendOtpCallback()
    }

    private val sendyOtpCheckCallback: CheckOtpCallback by lazy {
        SendyTestAppCheckOtpCallback()
    }

    var loginScreenState = mutableStateOf(
        LoginScreenState()
    )
        private set

    var otpScreenState = mutableStateOf(
        OtpScreenState()
    )
        private set

    fun sendCode(api: API, phoneNumber: String, context: Context) {
        loginScreenState.value = loginScreenState.value.copy(isSendSucceed = false)

        val mutex = Mutex()
        val job = Job()
        viewModelScope.launch(Dispatchers.IO + job) {
            launch {
                sendyOtpSendCallback.isSendSucceed.collect {
                    if (it) {
                        mutex.withLock {
                            loginScreenState.value = loginScreenState.value.copy(isSendSucceed = it, sendingError = null)
                            job.cancelChildren()
                        }
                    }
                }
            }

            launch {
                sendyOtpSendCallback.sendError.collect {
                    mutex.withLock {
                        loginScreenState.value = loginScreenState.value.copy(sendingError = it)
                        job.cancelChildren()
                    }
                }
            }
        }

        api.loginAtAuth(context, phoneNumber, sendyOtpSendCallback)
    }

    fun checkOtp(context: Context, api: API, otp: String) {
        val mutex = Mutex()
        val job = Job()
        viewModelScope.launch(Dispatchers.IO + job) {
            launch {
                sendyOtpCheckCallback.isCheckSucceed.collect {
                    if (it) {
                        mutex.withLock {
                            otpScreenState.value = otpScreenState.value.copy(isOtpConfirmed = it, otpConfirmError = null)
                            job.cancelChildren()
                        }
                    }
                }
            }

            launch {
                sendyOtpCheckCallback.checkError.collect {
                    mutex.withLock {
                        otpScreenState.value = otpScreenState.value.copy(otpConfirmError = it)
                        job.cancelChildren()
                    }
                }
            }
        }

        api.activateWllet(context, otp, TOKEN_TYPE, sendyOtpCheckCallback)
    }

    fun getTermOfUse(context: Context, api: API) {
        val job = Job()
        viewModelScope.launch(Dispatchers.IO + job) {
            launch {
                api.getTermsOfUse(context, sendyGetTermOfUseCallback)
                sendyGetTermOfUseCallback.htmlTermOfUse.collect {html->
                    val file = File(context.cacheDir, USER_AGREEMENT_FILE)
                    file.writeText(html, Charsets.UTF_8)
                    job.cancelChildren()
                }
            }
        }
    }

    fun showTermOfUse(context: Context) {
        val file = File(context.cacheDir, USER_AGREEMENT_FILE)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/html")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

}