package com.mason.sendytestapp

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking
import land.sendy.pfe_sdk.model.types.ApiCallback
import land.sendy.pfe_sdk.model.types.LoaderError
import land.sendy.pfe_sdk.model.types.ServerEventConfirm

abstract class OtpCallback: ApiCallback() {
    abstract var isSendSucceed: SharedFlow<Boolean>
    abstract var sendingError: SharedFlow<Throwable?>
}

class SendyCallback(): OtpCallback() {

    private val _isSendSucceed = MutableSharedFlow<Boolean>(replay = 0)
    override var isSendSucceed: SharedFlow<Boolean> = _isSendSucceed

    private val _sendingError = MutableSharedFlow<Throwable?>(replay = 0)
    override var sendingError: SharedFlow<Throwable?> = _sendingError

    override fun onFail(p0: LoaderError?) {
        runBlocking {
            //чекнуть типы ошибок, которые могут возникнуть
            _sendingError.emit(NullPointerException("send_error"))
        }
    }

    override fun onCompleted(p0: Boolean) {
        runBlocking {
            _isSendSucceed.emit(p0)
        }
    }
}

