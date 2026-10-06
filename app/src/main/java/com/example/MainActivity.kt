package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.MyApplicationTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

  private val viewModel: BrowserViewModel by viewModels()
  private var webViewRef: WebView? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Register native OnBackPressedCallback so hardware/gesture back navigates WebView history first
    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          val wv = webViewRef
          if (wv != null && wv.canGoBack()) {
            wv.goBack()
          } else {
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
            isEnabled = true
          }
        }
      },
    )

    setContent {
      MyApplicationTheme {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val baseContext = LocalContext.current
        val systemConfiguration = LocalConfiguration.current
        val systemLocale =
          remember(systemConfiguration) {
            val locales = systemConfiguration.locales
            if (!locales.isEmpty) locales[0] else Locale.getDefault()
          }
        val activeLocale =
          remember(uiState.languageOverride, systemLocale) {
            viewModel.getActiveLocale(systemLocale)
          }
        val localizedContext =
          remember(baseContext, activeLocale) {
            createLocalizedContext(baseContext, activeLocale)
          }

        CompositionLocalProvider(LocalContext provides localizedContext) {
          BrowserScreen(
            uiState = uiState,
            quickLinks = viewModel.quickLinks,
            systemLocale = systemLocale,
            activeLocale = activeLocale,
            onAddressChanged = viewModel::onAddressBarTextChanged,
            onClearAddress = viewModel::onClearAddressBar,
            onLoadInput = { rawInput ->
              val targetUrl = viewModel.resolveInputToUrl(rawInput)
              viewModel.onRetryInitiated()
              loadUrlWithLocaleHeader(webViewRef, targetUrl, activeLocale)
            },
            onNavigateBack = {
              if (webViewRef?.canGoBack() == true) {
                webViewRef?.goBack()
              }
            },
            onNavigateForward = {
              if (webViewRef?.canGoForward() == true) {
                webViewRef?.goForward()
              }
            },
            onReloadOrStop = {
              if (uiState.isLoading) {
                webViewRef?.stopLoading()
              } else {
                viewModel.onRetryInitiated()
                webViewRef?.reload()
              }
            },
            onNavigateHome = {
              viewModel.onRetryInitiated()
              loadUrlWithLocaleHeader(webViewRef, BrowserUiState.DEFAULT_HOME_URL, activeLocale)
            },
            onClearCache = {
              webViewRef?.clearCache(true)
              CookieManager.getInstance().removeAllCookies(null)
              CookieManager.getInstance().flush()
              Toast.makeText(
                  localizedContext,
                  localizedContext.getString(R.string.cache_cleared_toast),
                  Toast.LENGTH_SHORT,
                )
                .show()
            },
            onToggleLanguageDialog = viewModel::setLanguageDialogVisible,
            onSelectLanguageOverride = { override ->
              viewModel.setLanguageOverride(override)
            },
            onWebViewCreated = { webView ->
              webViewRef = webView
            },
            onPageStarted = viewModel::onPageStarted,
            onPageFinished = viewModel::onPageFinished,
            onProgressChanged = viewModel::onProgressChanged,
            onReceivedError = viewModel::onReceivedError,
          )
        }
      }
    }
  }

  override fun onDestroy() {
    webViewRef?.let { wv ->
      (wv.parent as? ViewGroup)?.removeView(wv)
      wv.stopLoading()
      wv.destroy()
    }
    webViewRef = null
    super.onDestroy()
  }
}

private fun createLocalizedContext(baseContext: Context, locale: Locale): Context {
  val config = Configuration(baseContext.resources.configuration)
  config.setLocale(locale)
  config.setLayoutDirection(locale)
  return baseContext.createConfigurationContext(config)
}

private fun loadUrlWithLocaleHeader(webView: WebView?, url: String, locale: Locale) {
  val langTag = locale.toLanguageTag()
  val headers = mapOf("Accept-Language" to "$langTag, ${locale.language};q=0.9, *;q=0.5")
  webView?.loadUrl(url, headers)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
  uiState: BrowserUiState,
  quickLinks: List<QuickLink>,
  systemLocale: Locale,
  activeLocale: Locale,
  onAddressChanged: (String) -> Unit,
  onClearAddress: () -> Unit,
  onLoadInput: (String) -> Unit,
  onNavigateBack: () -> Unit,
  onNavigateForward: () -> Unit,
  onReloadOrStop: () -> Unit,
  onNavigateHome: () -> Unit,
  onClearCache: () -> Unit,
  onToggleLanguageDialog: (Boolean) -> Unit,
  onSelectLanguageOverride: (AppLanguageOverride) -> Unit,
  onWebViewCreated: (WebView) -> Unit,
  onPageStarted: (String, Boolean, Boolean) -> Unit,
  onPageFinished: (String, String?, Boolean, Boolean) -> Unit,
  onProgressChanged: (Int, Boolean, Boolean) -> Unit,
  onReceivedError: (String?, Boolean, Boolean) -> Unit,
) {
  val focusManager = LocalFocusManager.current

  // Compose BackHandler so pressing Back navigates backward in WebView history when available
  BackHandler(enabled = uiState.canGoBack) {
    onNavigateBack()
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background,
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .windowInsetsPadding(WindowInsets.statusBars)
          .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
      // Top Browser Chrome: Address bar, Go button, Navigation & Locale controls
      BrowserTopToolbar(
        uiState = uiState,
        activeLocale = activeLocale,
        onAddressChanged = onAddressChanged,
        onClearAddress = onClearAddress,
        onSubmitUrl = {
          focusManager.clearFocus()
          onLoadInput(uiState.addressBarText)
        },
        onNavigateBack = onNavigateBack,
        onNavigateForward = onNavigateForward,
        onReloadOrStop = onReloadOrStop,
        onNavigateHome = onNavigateHome,
        onClearCache = onClearCache,
        onOpenLanguageDialog = { onToggleLanguageDialog(true) },
      )

      // Quick Bookmarks Strip + Detected Language Pill
      QuickLinksAndLocaleStrip(
        quickLinks = quickLinks,
        activeLocale = activeLocale,
        onQuickLinkClick = { url ->
          focusManager.clearFocus()
          onAddressChanged(url)
          onLoadInput(url)
        },
        onLanguageBadgeClick = { onToggleLanguageDialog(true) },
      )

      // Smooth Animated Page Load Progress Bar
      AnimatedVisibility(
        visible = uiState.isLoading,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        LinearProgressIndicator(
          progress = { (uiState.loadProgress.coerceIn(5, 100)) / 100f },
          modifier =
            Modifier.fillMaxWidth()
              .height(3.dp)
              .testTag("webview_progress_bar"),
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
      }

      HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
      )

      // Fullscreen Native WebView Container + Optional Error Overlay
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .weight(1f)
      ) {
        AndroidView(
          modifier =
            Modifier.fillMaxSize()
              .testTag("native_webview"),
          factory = { context ->
            WebView(context).apply {
              layoutParams =
                ViewGroup.LayoutParams(
                  ViewGroup.LayoutParams.MATCH_PARENT,
                  ViewGroup.LayoutParams.MATCH_PARENT,
                )

              // Configure WebView Settings: Enable JavaScript, DOM Storage, and Caching
              settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                loadsImagesAutomatically = true
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                javaScriptCanOpenWindowsAutomatically = false
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
              }

              // Enable cookies for modern web compatibility
              CookieManager.getInstance().setAcceptCookie(true)
              CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

              // Custom WebViewClient so all links open inside the same WebView
              webViewClient =
                object : WebViewClient() {
                  override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?,
                  ): Boolean {
                    val targetUri = request?.url ?: return false
                    val scheme = targetUri.scheme?.lowercase(Locale.ROOT)
                    return if (scheme == "http" || scheme == "https") {
                      // Return false so WebView loads the URL internally instead of launching an external browser
                      false
                    } else {
                      // Block non-web schemes from launching external intents automatically
                      true
                    }
                  }

                  @Deprecated("Deprecated in Java")
                  override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (url == null) return false
                    val lower = url.lowercase(Locale.ROOT)
                    return if (lower.startsWith("http://") || lower.startsWith("https://")) {
                      false
                    } else {
                      true
                    }
                  }

                  override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    if (url != null && view != null) {
                      onPageStarted(url, view.canGoBack(), view.canGoForward())
                    }
                  }

                  override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (url != null && view != null) {
                      onPageFinished(url, view.title, view.canGoBack(), view.canGoForward())
                    }
                  }

                  override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?,
                  ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true && view != null) {
                      onReceivedError(
                        error?.description?.toString(),
                        view.canGoBack(),
                        view.canGoForward(),
                      )
                    }
                  }
                }

              // Custom WebChromeClient for progress bar and page title updates
              webChromeClient =
                object : WebChromeClient() {
                  override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    if (view != null) {
                      onProgressChanged(newProgress, view.canGoBack(), view.canGoForward())
                    }
                  }
                }

              onWebViewCreated(this)
              loadUrlWithLocaleHeader(this, BrowserUiState.DEFAULT_HOME_URL, activeLocale)
            }
          },
          update = { webView ->
            onWebViewCreated(webView)
          },
        )

        // Friendly Localized Error Overlay if main frame fails to load
        if (uiState.hasError) {
          BrowserErrorOverlay(
            errorDetail = uiState.errorDescription,
            onRetry = onReloadOrStop,
            onGoHome = onNavigateHome,
          )
        }
      }
    }

    if (uiState.isLanguageDialogVisible) {
      LanguageSelectionDialog(
        currentOverride = uiState.languageOverride,
        systemLocale = systemLocale,
        onSelectOverride = onSelectLanguageOverride,
        onDismiss = { onToggleLanguageDialog(false) },
      )
    }
  }
}

@Composable
private fun BrowserTopToolbar(
  uiState: BrowserUiState,
  activeLocale: Locale,
  onAddressChanged: (String) -> Unit,
  onClearAddress: () -> Unit,
  onSubmitUrl: () -> Unit,
  onNavigateBack: () -> Unit,
  onNavigateForward: () -> Unit,
  onReloadOrStop: () -> Unit,
  onNavigateHome: () -> Unit,
  onClearCache: () -> Unit,
  onOpenLanguageDialog: () -> Unit,
) {
  var isAddressFocused by remember { mutableStateOf(false) }

  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 3.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      // Row 1: Navigation Controls + Omnibox Address Bar + Go Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        IconButton(
          onClick = onNavigateBack,
          enabled = uiState.canGoBack,
          modifier =
            Modifier.size(48.dp)
              .minimumInteractiveComponentSize()
              .testTag("nav_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.cd_navigate_back),
          )
        }

        IconButton(
          onClick = onNavigateForward,
          enabled = uiState.canGoForward,
          modifier =
            Modifier.size(48.dp)
              .minimumInteractiveComponentSize()
              .testTag("nav_forward_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = stringResource(R.string.cd_navigate_forward),
          )
        }

        IconButton(
          onClick = onReloadOrStop,
          modifier =
            Modifier.size(48.dp)
              .minimumInteractiveComponentSize()
              .testTag("nav_reload_button"),
        ) {
          Icon(
            imageVector = if (uiState.isLoading) Icons.Default.Close else Icons.Default.Refresh,
            contentDescription =
              if (uiState.isLoading) {
                stringResource(R.string.cd_stop_loading)
              } else {
                stringResource(R.string.cd_reload_page)
              },
          )
        }

        // Address Bar Input Capsule
        Row(
          modifier =
            Modifier.weight(1f)
              .height(48.dp)
              .clip(RoundedCornerShape(24.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .border(
                width = if (isAddressFocused) 1.5.dp else 1.dp,
                color =
                  if (isAddressFocused) {
                    MaterialTheme.colorScheme.primary
                  } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                  },
                shape = RoundedCornerShape(24.dp),
              )
              .padding(start = 12.dp, end = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = if (uiState.isSecure) Icons.Default.Lock else Icons.Default.LockOpen,
            contentDescription =
              if (uiState.isSecure) {
                stringResource(R.string.cd_security_secure)
              } else {
                stringResource(R.string.cd_security_unsecure)
              },
            tint =
              if (uiState.isSecure) {
                MaterialTheme.colorScheme.secondary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
            modifier = Modifier.size(16.dp),
          )

          Spacer(modifier = Modifier.width(8.dp))

          Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
          ) {
            if (uiState.addressBarText.isEmpty()) {
              Text(
                text = stringResource(R.string.address_bar_hint),
                style =
                  MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                  ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            BasicTextField(
              value = uiState.addressBarText,
              onValueChange = onAddressChanged,
              singleLine = true,
              textStyle =
                TextStyle(
                  color = MaterialTheme.colorScheme.onSurface,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Normal,
                ),
              cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
              keyboardOptions =
                KeyboardOptions(
                  keyboardType = KeyboardType.Uri,
                  imeAction = ImeAction.Go,
                ),
              keyboardActions =
                KeyboardActions(
                  onGo = { onSubmitUrl() },
                ),
              modifier =
                Modifier.fillMaxWidth()
                  .onFocusChanged { focusState ->
                    isAddressFocused = focusState.isFocused
                  }
                  .testTag("address_bar_input"),
            )
          }

          if (uiState.addressBarText.isNotEmpty()) {
            IconButton(
              onClick = onClearAddress,
              modifier =
                Modifier.size(40.dp)
                  .testTag("clear_address_button"),
            ) {
              Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = stringResource(R.string.cd_clear_url),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }

        // Go / Перейти Action Button
        Button(
          onClick = onSubmitUrl,
          shape = RoundedCornerShape(24.dp),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
          modifier =
            Modifier.height(48.dp)
              .minimumInteractiveComponentSize()
              .testTag("go_button"),
        ) {
          Text(
            text = stringResource(R.string.action_go),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
          )
        }
      }
    }
  }
}

@Composable
private fun QuickLinksAndLocaleStrip(
  quickLinks: List<QuickLink>,
  activeLocale: Locale,
  onQuickLinkClick: (String) -> Unit,
  onLanguageBadgeClick: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 12.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // Live Detected Language Badge (Interactive)
      Row(
        modifier =
          Modifier.height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
            .clickable(onClick = onLanguageBadgeClick)
            .padding(horizontal = 12.dp)
            .testTag("language_badge_button"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Default.Language,
          contentDescription = stringResource(R.string.cd_language_settings),
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text =
            stringResource(
              R.string.language_detected_format,
              activeLocale.getDisplayLanguage(activeLocale).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(activeLocale) else it.toString()
              },
              activeLocale.language.uppercase(Locale.ROOT),
            ),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          fontWeight = FontWeight.SemiBold,
        )
      }

      // Quick Bookmark Chips
      quickLinks.forEach { link ->
        Row(
          modifier =
            Modifier.height(48.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
              .clickable { onQuickLinkClick(link.url) }
              .padding(horizontal = 12.dp)
              .testTag("quick_link_${link.domainBadge.lowercase(Locale.ROOT)}"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Box(
            modifier =
              Modifier.size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = link.domainBadge,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Text(
            text = stringResource(link.titleResId),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun BrowserErrorOverlay(
  errorDetail: String?,
  onRetry: () -> Unit,
  onGoHome: () -> Unit,
) {
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(24.dp)
        .testTag("error_overlay"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        imageVector = Icons.Default.WarningAmber,
        contentDescription = stringResource(R.string.error_page_load_title),
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(56.dp),
      )
      Text(
        text = stringResource(R.string.error_page_load_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
      )
      Text(
        text = stringResource(R.string.error_page_load_message),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      if (!errorDetail.isNullOrBlank()) {
        Text(
          text = errorDetail,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
          textAlign = TextAlign.Center,
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
          onClick = onGoHome,
          modifier =
            Modifier.height(48.dp)
              .testTag("error_home_button"),
        ) {
          Icon(
            imageVector = Icons.Default.Home,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = stringResource(R.string.action_go_home))
        }
        FilledTonalButton(
          onClick = onRetry,
          modifier =
            Modifier.height(48.dp)
              .testTag("error_retry_button"),
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = stringResource(R.string.action_retry))
        }
      }
    }
  }
}

@Composable
private fun LanguageSelectionDialog(
  currentOverride: AppLanguageOverride,
  systemLocale: Locale,
  onSelectOverride: (AppLanguageOverride) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.language_dialog_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = stringResource(R.string.language_dialog_subtitle),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))

        val options =
          listOf(
            AppLanguageOverride.SYSTEM_AUTO to
              stringResource(
                R.string.language_system_default,
                systemLocale.language.uppercase(Locale.ROOT),
              ),
            AppLanguageOverride.ENGLISH to stringResource(R.string.language_english),
            AppLanguageOverride.RUSSIAN to stringResource(R.string.language_russian),
          )

        options.forEach { (option, label) ->
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onSelectOverride(option) }
                .padding(horizontal = 8.dp)
                .testTag("lang_option_${option.name.lowercase(Locale.ROOT)}"),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(
              selected = currentOverride == option,
              onClick = { onSelectOverride(option) },
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = label,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("language_dialog_close_button"),
      ) {
        Text(text = stringResource(R.string.action_close))
      }
    },
  )
}
