package com.mason.sendytestapp

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import land.sendy.pfe_sdk.api.API
import java.io.File

data class OtpSendStatus(
    var sendingError: Throwable? = null,
    var isSendSucceed: Boolean = false,
)

data class OtpConfirmStatus(
    var otpConfirmError: Throwable? = null,
    var otpConfirmResult: Pair<Boolean, Int> = Pair(false, TRY_OTP_CONFIRM_COUNT)
) {
    companion object {
        const val TRY_OTP_CONFIRM_COUNT = 3
    }
}

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

    private val _phoneScreenOtpSendStatusFlow = MutableSharedFlow<OtpSendStatus>(replay = 1)
    val loginScreenFlow = _phoneScreenOtpSendStatusFlow.asSharedFlow()

    fun sendCode(api: API, phoneNumber: String, context: Context) {
        setUpSendOtpCallbackCollector(_phoneScreenOtpSendStatusFlow)
        api.loginAtAuth(context, phoneNumber, sendyOtpSendCallback)
    }

    private val _otpResendStatusFlow = MutableSharedFlow<OtpSendStatus>(replay = 1)
    val otpResendStatusFlow = _otpResendStatusFlow.asSharedFlow()

    fun resetOtp(api: API, phoneNumber: String, context: Context) {
        viewModelScope.launch {
            _otpConfirmStatusFlow.emit(OtpConfirmStatus())
            setUpSendOtpCallbackCollector(_otpResendStatusFlow)
            api.loginAtAuth(context, phoneNumber, sendyOtpSendCallback)
        }
    }

    private val _otpConfirmStatusFlow = MutableSharedFlow<OtpConfirmStatus>(replay = 1)
    val otpConfirmStatusFlow = _otpConfirmStatusFlow.asSharedFlow()

    fun checkOtp(context: Context, api: API, otp: String) {
        val job = Job()
        viewModelScope.launch(Dispatchers.IO + job) {
            val previousState = _otpConfirmStatusFlow.replayCache.firstOrNull()
            launch {
                sendyOtpCheckCallback.confirmResult.collect { result->
                    val newState = previousState?.copy(
                        otpConfirmResult = result,
                        otpConfirmError = null
                    ) ?: OtpConfirmStatus(otpConfirmResult = result)

                    _otpConfirmStatusFlow.emit(newState)

                    job.cancelChildren()
                }
            }

            launch {
                sendyOtpCheckCallback.confirmError.collect {
                    val newState = previousState?.copy(otpConfirmError = it) ?: OtpConfirmStatus(otpConfirmError = it)

                    _otpConfirmStatusFlow.emit(newState)

                    job.cancelChildren()
                }
            }
        }
        api.activateWllet(context, otp, TOKEN_TYPE, sendyOtpCheckCallback)
    }

    private fun setUpSendOtpCallbackCollector(flowForCollect: MutableSharedFlow<OtpSendStatus>) {
        val job = Job()

        viewModelScope.launch(Dispatchers.IO + job) {
            val previousState = flowForCollect.replayCache.firstOrNull()

            launch {
                sendyOtpSendCallback.sendResult.collect {
                    val newState = previousState?.copy(isSendSucceed = it, sendingError = null)
                        ?: OtpSendStatus(isSendSucceed = it, sendingError = null)
                    flowForCollect.emit(newState)
                    job.cancelChildren()
                }
            }

            launch {
                sendyOtpSendCallback.sendError.collect {
                    val newState = previousState?.copy(sendingError = it)
                        ?: OtpSendStatus(sendingError = it)
                    flowForCollect.emit(newState)
                    job.cancelChildren()
                }
            }
        }
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