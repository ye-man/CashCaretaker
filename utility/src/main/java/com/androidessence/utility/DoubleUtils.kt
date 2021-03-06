package com.androidessence.utility

import java.text.NumberFormat
import java.util.*

/**
 * Extension method for Doubles
 */

fun Double.asCurrency(): String = NumberFormat.getCurrencyInstance(Locale.getDefault()).format(this)