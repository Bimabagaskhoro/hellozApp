package org.app.projectxyz.hellozapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import org.app.projectxyz.hellozapp.theme.AppTheme
import org.app.projectxyz.hellozapp.uicomponent.webview.WebView
import org.app.projectxyz.hellozapp.uicomponent.webview.rememberWebViewState

@Composable
internal fun AppWebView() = AppTheme {
    val state = rememberWebViewState(
        url = "https://github.com/bimabagaskhoro"
    )

    LaunchedEffect(state.isLoading) {
        // Get the current loading state
    }

    WebView(
        state = state,
        modifier = Modifier
            .fillMaxSize()
    )

    LaunchedEffect(Unit) {
        state.settings.javaScriptEnabled = true

        state.settings.androidSettings.supportZoom = false
    }
}