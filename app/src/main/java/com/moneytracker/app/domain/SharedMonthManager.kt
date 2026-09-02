// Source reference
package com.moneytracker.app.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedMonthManager @Inject constructor() {
    
    // Null represents "All Time"
    private val _currentMonth = MutableStateFlow<Calendar?>(Calendar.getInstance())
    val currentMonth: StateFlow<Calendar?> = _currentMonth.asStateFlow()

    fun previousMonth() {
        _currentMonth.update { cal ->
            if (cal == null) {
                Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
            } else {
                (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            }
        }
    }

    fun nextMonth() {
        _currentMonth.update { cal ->
            if (cal == null) {
                Calendar.getInstance().apply { add(Calendar.MONTH, 1) }
            } else {
                (cal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            }
        }
    }

    fun selectAllTime() {
        _currentMonth.value = null
    }

    // Resets the month tracker back to the current date
    fun resetToCurrentMonth() {
        _currentMonth.value = Calendar.getInstance()
    }
}