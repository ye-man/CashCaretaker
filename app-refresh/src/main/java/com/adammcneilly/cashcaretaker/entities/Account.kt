package com.adammcneilly.cashcaretaker.entities

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * A bank account that the user may have.
 */
@Entity
data class Account(
        @PrimaryKey(autoGenerate = false) var name: String = "",
        var balance: Double = 0.0)