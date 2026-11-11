package com.dairypulse.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.dairypulse.app.DairyPulseApp

@Composable
fun rememberApp(): DairyPulseApp = LocalContext.current.applicationContext as DairyPulseApp
