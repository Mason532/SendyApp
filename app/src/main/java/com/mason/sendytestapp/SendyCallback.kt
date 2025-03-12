package com.mason.sendytestapp

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking
import land.sendy.pfe_sdk.api.API
import land.sendy.pfe_sdk.model.pfe.response.TermsOfUseRs
import land.sendy.pfe_sdk.model.types.ApiCallback
import land.sendy.pfe_sdk.model.types.LoaderError

abstract class CheckOtpCallback: ApiCallback() {
    abstract var isCheckSucceed: SharedFlow<Boolean>
    abstract var checkError: SharedFlow<Throwable?>
}

abstract class SendOtpCallback: ApiCallback() {
    abstract var isSendSucceed: SharedFlow<Boolean>
    abstract var sendError: SharedFlow<Throwable?>
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
    override var isSendSucceed: SharedFlow<Boolean> = _isSendSucceed

    private val _sendError = MutableSharedFlow<Throwable?>(replay = 0)
    override var sendError: SharedFlow<Throwable?> = _sendError

    override fun onFail(p0: LoaderError?) {
        runBlocking {
            _sendError.emit(NullPointerException("send_error"))
        }
    }

    override fun onCompleted(p0: Boolean) {
        runBlocking {
            _isSendSucceed.emit(p0)
        }
    }
}

class SendyTestAppCheckOtpCallback(): CheckOtpCallback() {
    private val _isCheckSucceed = MutableSharedFlow<Boolean>(replay = 0)
    override var isCheckSucceed: SharedFlow<Boolean> = _isCheckSucceed

    private val _checkError = MutableSharedFlow<Throwable?>(replay = 0)
    override var checkError: SharedFlow<Throwable?> = _checkError

    override fun onFail(p0: LoaderError?) {
        runBlocking {
            _checkError.emit(NullPointerException("check_error"))
        }
    }

    override fun onCompleted(p0: Boolean) {
        runBlocking {
            _isCheckSucceed.emit(p0)
        }
    }
}

