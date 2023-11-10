package org.app.projectxyz.hellozapp.uicomponent.webview

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Foundation.setValue
import platform.Foundation.dataUsingEncoding
import platform.Foundation.NSURL
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSUTF8StringEncoding
import platform.WebKit.*
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun WebView(
    state: WebViewState,
    modifier: Modifier,
    captureBackPresses: Boolean,
    navigator: WebViewNavigator,
    onCreated: () -> Unit,
    onDispose: () -> Unit,
) {
    val webView = state.webView

    webView?.let { wv ->
        LaunchedEffect(wv, navigator) {
            navigator.handleNavigationEvents(wv)
        }

        LaunchedEffect(wv, state) {
            snapshotFlow { state.content }.collect { content ->
                when (content) {
                    is WebContent.Url -> {
                        val url = NSURL(string = content.url)
                        val urlRequest = NSMutableURLRequest()
                        urlRequest.setURL(url)
                        content.additionalHttpHeaders.forEach { (key, value) ->
                            urlRequest.setValue(value = value, forHTTPHeaderField = key)
                        }
                        wv.loadRequest(urlRequest)
                        wv.allowsBackForwardNavigationGestures = true
                    }

                    is WebContent.Data -> {
                        val baseUrl = content.baseUrl?.let { NSURL(string = it) }

                        wv.loadHTMLString(
                            content.data,
                            baseUrl
                        )
                    }

                    is WebContent.NavigatorOnly -> {
                    }

                    else -> {}
                }
            }
        }
    }

    UIKitView(
        factory = {
            WKWebView().apply {
                onCreated()
                setUserInteractionEnabled(captureBackPresses)
                applySettings(state.settings)
                state.webView = this
                navigationDelegate = state
            }
        },
        onRelease = {
            onDispose()
            state.webView = null
        },
        modifier = modifier,
    )
}

@Stable
actual class WebViewState actual constructor(
    webContent: WebContent
): NSObject(), WKNavigationDelegateProtocol {
    actual var lastLoadedUrl: String? by mutableStateOf(null)
        internal set

    actual var content: WebContent by mutableStateOf(webContent)

    actual var loadingState: LoadingState by mutableStateOf(LoadingState.Initializing)
        internal set

    actual val isLoading: Boolean
        get() = loadingState !is LoadingState.Finished

    actual var pageTitle: String? by mutableStateOf(null)
        internal set

    actual val settings: WebSettings = WebSettings(
        onSettingsChanged = {
            applySetting()
        }
    )

    private fun applySetting() {
        webView?.applySettings(settings)
    }

    actual fun evaluateJavascript(script: String, callback: ((String?) -> Unit)?) {
        webView?.evaluateJavaScript(script) { result, error ->
            if (error != null) {
                callback?.invoke(error.localizedDescription())
            } else {
                callback?.invoke(result?.toString())
            }
        }
    }

    var webView by mutableStateOf<WKWebView?>(null)
        internal set

    @Suppress("CONFLICTING_OVERLOADS")
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        loadingState = LoadingState.Finished
    }

    @Suppress("CONFLICTING_OVERLOADS")
    override fun webView(webView: WKWebView, didCommitNavigation: WKNavigation?) {
        loadingState = LoadingState.Loading(webView.estimatedProgress.toFloat())
    }
}

private fun WKWebView.applySettings(webSettings: WebSettings) {
    configuration.defaultWebpagePreferences.allowsContentJavaScript = webSettings.javaScriptEnabled
    configuration.preferences.javaScriptEnabled = webSettings.javaScriptEnabled
    configuration.preferences.javaScriptCanOpenWindowsAutomatically = webSettings.javaScriptCanOpenWindowsAutomatically
}

internal suspend fun WebViewNavigator.handleNavigationEvents(
    webView: WKWebView
): Nothing = withContext(Dispatchers.Main) {
    navigationEvents.collect { event ->
        when (event) {
            is WebViewNavigator.NavigationEvent.Back -> webView.goBack()
            is WebViewNavigator.NavigationEvent.Forward -> webView.goForward()
            is WebViewNavigator.NavigationEvent.Reload -> webView.reload()
            is WebViewNavigator.NavigationEvent.StopLoading -> webView.stopLoading()
            is WebViewNavigator.NavigationEvent.LoadHtml -> {
                val data = NSString
                    .create(string = event.html)
                    .dataUsingEncoding(NSUTF8StringEncoding) ?: return@collect
                val baseUrl =
                    if (event.baseUrl != null) NSURL(string = event.baseUrl)
                    else return@collect

                webView.loadData(
                    data,
                    event.mimeType ?: "text/html",
                    event.encoding ?: "utf-8",
                    baseUrl
                )
            }

            is WebViewNavigator.NavigationEvent.LoadUrl -> {
                loadUrl(event.url, event.additionalHttpHeaders)
            }
        }
    }
}

actual val WebStateSaver: Saver<WebViewState, Any> = run {
    val pageTitleKey = "pagetitle"
    val lastLoadedUrlKey = "lastloaded"

    mapSaver(
        save = {
            mapOf(
                pageTitleKey to it.pageTitle,
                lastLoadedUrlKey to it.lastLoadedUrl,
            )
        },
        restore = {
            WebViewState(WebContent.NavigatorOnly).apply {
                this.pageTitle = it[pageTitleKey] as String?
                this.lastLoadedUrl = it[lastLoadedUrlKey] as String?
            }
        }
    )
}