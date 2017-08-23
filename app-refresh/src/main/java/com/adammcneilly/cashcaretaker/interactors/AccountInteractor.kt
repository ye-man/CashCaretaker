package com.adammcneilly.cashcaretaker.interactors

import com.adammcneilly.cashcaretaker.entities.Account

/**
 * Interface for handling account loading.
 */
interface AccountInteractor {
    interface OnFinishedListener {
        fun onFinished(accounts: List<Account>)
    }

    fun getAll(listener: OnFinishedListener)
}