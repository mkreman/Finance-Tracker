package com.moneytracker.app.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val MAX_PASSCODE_ATTEMPTS = 5

private fun lockoutDurationForLevel(level: Int): Long {
    return when {
        level <= 1 -> 60_000L
        level == 2 -> 5 * 60_000L
        else -> 15 * 60_000L
    }
}

@Composable
fun PasscodeScreen(
    storedPasscode: String,
    initialLockoutEndTime: Long = 0L,
    initialLockoutLevel: Int = 0,
    onLockoutStateChange: (Long, Int) -> Unit = { _, _ -> },
    onLockoutReset: () -> Unit = {},
    onSuccess: () -> Unit,
    onBiometricClick: (() -> Unit)? = null,
    showBiometricOption: Boolean = false
) {
    val context = LocalContext.current
    var enteredPasscode by remember { mutableStateOf("") }
    var failedAttempts by rememberSaveable { mutableIntStateOf(0) }
    var lockoutEndTime by remember(initialLockoutEndTime) { mutableLongStateOf(initialLockoutEndTime) }
    var lockoutLevel by remember(initialLockoutLevel) { mutableIntStateOf(initialLockoutLevel) }
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(initialLockoutEndTime, initialLockoutLevel) {
        lockoutEndTime = initialLockoutEndTime
        lockoutLevel = initialLockoutLevel
    }

    val isLocked = lockoutEndTime > currentTimeMillis
    val remainingSeconds = if (isLocked) {
        ((lockoutEndTime - currentTimeMillis + 999L) / 1000L).toInt().coerceAtLeast(0)
    } else {
        0
    }

    LaunchedEffect(lockoutEndTime) {
        while (lockoutEndTime > 0L && System.currentTimeMillis() < lockoutEndTime) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000)
        }
        if (lockoutEndTime > 0L && System.currentTimeMillis() >= lockoutEndTime) {
            currentTimeMillis = System.currentTimeMillis()
            lockoutEndTime = 0L
            lockoutLevel = 0
            failedAttempts = 0
            onLockoutReset()
        }
    }

    fun onPasscodeDigitEntered(digit: String) {
        if (isLocked || enteredPasscode.length >= 4) return

        enteredPasscode += digit
        if (enteredPasscode.length != 4) return

        if (enteredPasscode == storedPasscode) {
            failedAttempts = 0
            lockoutLevel = 0
            lockoutEndTime = 0L
            onLockoutReset()
            onSuccess()
        } else {
            failedAttempts += 1
            if (failedAttempts >= MAX_PASSCODE_ATTEMPTS) {
                lockoutLevel += 1
                val lockoutDuration = lockoutDurationForLevel(lockoutLevel)
                val newLockoutEndTime = System.currentTimeMillis() + lockoutDuration
                lockoutEndTime = newLockoutEndTime
                currentTimeMillis = System.currentTimeMillis()
                onLockoutStateChange(newLockoutEndTime, lockoutLevel)
                Toast.makeText(
                    context,
                    "Too many wrong attempts. App is locked for ${lockoutDuration / 60000} minute(s).",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val attemptsLeft = MAX_PASSCODE_ATTEMPTS - failedAttempts
                Toast.makeText(
                    context,
                    "Incorrect passcode. $attemptsLeft attempts left",
                    Toast.LENGTH_SHORT
                ).show()
            }
            enteredPasscode = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = "Lock",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Enter Passcode",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Unlock Money Tracker",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isLocked) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Too many wrong attempts. Try again in ${remainingSeconds}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Passcode dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (index < enteredPasscode.length)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Number pad
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Rows 1-3
                for (row in 0..2) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (col in 1..3) {
                            val number = row * 3 + col
                            NumberButton(
                                number = number.toString(),
                                onClick = {
                                    onPasscodeDigitEntered(number.toString())
                                }
                            )
                        }
                    }
                }

                // Row 4: biometric/empty, 0, backspace
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left button (biometric or empty)
                    if (showBiometricOption && onBiometricClick != null) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(enabled = !isLocked) { onBiometricClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Use Biometric",
                                tint = if (isLocked) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(72.dp))
                    }

                    // 0
                    NumberButton(
                        number = "0",
                        onClick = {
                            onPasscodeDigitEntered("0")
                        }
                    )

                    // Backspace
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(enabled = !isLocked) {
                                if (enteredPasscode.isNotEmpty()) {
                                    enteredPasscode = enteredPasscode.dropLast(1)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backspace,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberButton(number: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
