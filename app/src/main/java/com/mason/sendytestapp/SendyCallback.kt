package com.mason.sendytestapp

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking
import land.sendy.pfe_sdk.api.API
import land.sendy.pfe_sdk.model.pfe.response.AuthActivateRs
import land.sendy.pfe_sdk.model.pfe.response.BResponse
import land.sendy.pfe_sdk.model.pfe.response.TermsOfUseRs
import land.sendy.pfe_sdk.model.types.ApiCallback
import land.sendy.pfe_sdk.model.types.LoaderError

abstract class CheckOtpCallback: ApiCallback() {
    abstract var confirmResult: SharedFlow<Pair<Boolean, Int>>
    abstract var confirmError: SharedFlow<Throwable?>
}

abstract class SendOtpCallback: ApiCallback() {
    abstract var sendResult: SharedFlow<Boolean>
    abstract var sendError: SharedFlow<Throwable?>
    abstract fun resetCallbackState()
}

abstract class TermOfUseCallback: ApiCallback() {
    abstract var htmlTermOfUse: SharedFlow<String>
}

class SendyTestAppTermOfUseCallback(): TermOfUseCallback() {
    private val _htmlTermOfUse = MutableSharedFlow<String>(replay = 0)
    override var htmlTermOfUse: SharedFlow<String> = _htmlTermOfUse
    override fun onCompleted(p0: Boolean) {
        if (!p0 || errNo != 0) {
            API.outLog("____$this")
        } else {
            val html = (this.oResponse as TermsOfUseRs).TextTermsOfUse
            runBlocking {
                _htmlTermOfUse.emit(html)
            }
        }
    }
}

class SendyTestAppSendOtpCallback(): SendOtpCallback() {
    private val _isSendSucceed = MutableSharedFlow<Boolean>(replay = 0)
    override var sendResult: SharedFlow<Boolean> = _isSendSucceed

    private val _sendError = MutableSharedFlow<Throwable?>(replay = 0)
    override var sendError: SharedFlow<Throwable?> = _sendError

    override fun resetCallbackState() {
        TODO("Not yet implemented")
    }

    private val npErr = NullPointerException("send_error")

    override fun onFail(p0: LoaderError?) {
        runBlocking {
            _sendError.emit(npErr)
        }

    }

    override fun onCompleted(p0: Boolean) {
        runBlocking {
            if (p0) {
                _isSendSucceed.emit(p0)
            } else {
                _sendError.emit(npErr)
            }
        }
    }
}

class SendyTestAppCheckOtpCallback(): CheckOtpCallback() {
    private val _isCheckSucceed = MutableSharedFlow<Pair<Boolean, Int>>(replay = 0)
    override var confirmResult: SharedFlow<Pair<Boolean, Int>> = _isCheckSucceed

    private val _checkError = MutableSharedFlow<Throwable?>(replay = 0)
    override var confirmError: SharedFlow<Throwable?> = _checkError

    private val npErr = NullPointerException("check_error")

    companion object {
        const val TRY_OTP_CONFIRM_COUNT = 3
    }

    override fun onFail(p0: LoaderError?) {
        runBlocking {
            _checkError.emit(npErr)
        }
    }

    private var otpConfirmResult: Pair<Boolean, Int> = Pair(false, TRY_OTP_CONFIRM_COUNT)

    override fun <T : BResponse?> onSuccess(data: T) {
        super.onSuccess(data)
        if (data != null) {
            if (this.errNo == 0) {
                /*
                val response = this.oResponse as AuthActivateRs
                if (response.PANs == null) {
                    //выбрать валюту для кошелька
                }

                if (response.Active != null && response.Active) {
                    Log.d("yes", "true")
                } else {
                    Log.d("no", "false")
                }
                 */
                otpConfirmResult = otpConfirmResult.copy(true)
            }
            else {
                val response = this.oResponse as? AuthActivateRs
                val restOtpConfirmTry = otpConfirmResult.second

                otpConfirmResult = otpConfirmResult.copy(false,
                    response?.Abuse ?: if (restOtpConfirmTry > 0) restOtpConfirmTry - 1
                    else restOtpConfirmTry
                )
            }
            runBlocking {
                _isCheckSucceed.emit(otpConfirmResult)
            }

        } else {
            runBlocking {
                _checkError.emit(npErr)
            }
        }
    }

}

