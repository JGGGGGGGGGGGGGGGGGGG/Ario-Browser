package com.example

import android.util.Patterns
import androidx.lifecycle.ViewModel
import java.net.URLEncoder
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AppLanguageOverride(val languageTag: String?) {
  SYSTEM_AUTO(null),
  ENGLISH("en"),
  RUSSIAN("ru"),
}

data class QuickLink(
  val titleResId: Int,
  val url: String,
  val domainBadge: String,
)

data class BrowserUiState(
  val currentUrl: String = DEFAULT_HOME_URL,
  val addressBarText: String = DEFAULT_HOME_URL,
  val pageTitle: String = "",
  val isLoading: Boolean = false,
  val loadProgress: Int = 0,
  val canGoBack: Boolean = false,
  val canGoForward: Boolean = false,
  val isSecure: Boolean = true,
  val hasError: Boolean = false,
  val errorDescription: String? = null,
  val languageOverride: AppLanguageOverride = AppLanguageOverride.SYSTEM_AUTO,
  val isLanguageDialogVisible: Boolean = false,
) {
  companion object {
    const val DEFAULT_HOME_URL = "https://www.google.com"
  }
}

class BrowserViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(BrowserUiState())
  val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

  val quickLinks =
    listOf(
      QuickLink(R.string.quick_link_google, "https://www.google.com", "G"),
      QuickLink(R.string.quick_link_wikipedia, "https://www.wikipedia.org", "W"),
      QuickLink(R.string.quick_link_duckduckgo, "https://duckduckgo.com", "D"),
      QuickLink(R.string.quick_link_github, "https://github.com", "GH"),
    )

  fun onAddressBarTextChanged(newText: String) {
    _uiState.update { it.copy(addressBarText = newText) }
  }

  fun onClearAddressBar() {
    _uiState.update { it.copy(addressBarText = "") }
  }

  fun resolveInputToUrl(rawInput: String): String {
    val trimmed = rawInput.trim()
    if (trimmed.isEmpty()) {
      return BrowserUiState.DEFAULT_HOME_URL
    }
    if (trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true)
    ) {
      return trimmed
    }
    // Check if it looks like a domain or IP address without spaces
    val looksLikeDomain =
      !trimmed.contains(" ") &&
        (trimmed.contains(".") || trimmed.startsWith("localhost")) &&
        (Patterns.WEB_URL.matcher(trimmed).matches() ||
          Patterns.WEB_URL.matcher("https://$trimmed").matches())

    return if (looksLikeDomain) {
      "https://$trimmed"
    } else {
      val encodedQuery =
        try {
          URLEncoder.encode(trimmed, "UTF-8")
        } catch (_: Exception) {
          trimmed.replace(" ", "+")
        }
      "https://www.google.com/search?q=$encodedQuery"
    }
  }

  fun onPageStarted(url: String, canGoBack: Boolean, canGoForward: Boolean) {
    _uiState.update {
      it.copy(
        currentUrl = url,
        addressBarText = url,
        isLoading = true,
        hasError = false,
        errorDescription = null,
        canGoBack = canGoBack,
        canGoForward = canGoForward,
        isSecure = url.startsWith("https://", ignoreCase = true),
      )
    }
  }

  fun onPageFinished(url: String, title: String?, canGoBack: Boolean, canGoForward: Boolean) {
    _uiState.update {
      it.copy(
        currentUrl = url,
        addressBarText = url,
        pageTitle = title.orEmpty(),
        isLoading = false,
        loadProgress = 100,
        canGoBack = canGoBack,
        canGoForward = canGoForward,
        isSecure = url.startsWith("https://", ignoreCase = true),
      )
    }
  }

  fun onProgressChanged(progress: Int, canGoBack: Boolean, canGoForward: Boolean) {
    _uiState.update {
      it.copy(
        loadProgress = progress,
        isLoading = progress in 1..99,
        canGoBack = canGoBack,
        canGoForward = canGoForward,
      )
    }
  }

  fun onReceivedError(errorMessage: String?, canGoBack: Boolean, canGoForward: Boolean) {
    _uiState.update {
      it.copy(
        isLoading = false,
        hasError = true,
        errorDescription = errorMessage,
        canGoBack = canGoBack,
        canGoForward = canGoForward,
      )
    }
  }

  fun onRetryInitiated() {
    _uiState.update { it.copy(hasError = false, errorDescription = null, isLoading = true) }
  }

  fun setLanguageDialogVisible(visible: Boolean) {
    _uiState.update { it.copy(isLanguageDialogVisible = visible) }
  }

  fun setLanguageOverride(override: AppLanguageOverride) {
    _uiState.update {
      it.copy(languageOverride = override, isLanguageDialogVisible = false)
    }
  }

  fun getActiveLocale(systemLocale: Locale): Locale {
    val tag = _uiState.value.languageOverride.languageTag
    return if (tag != null) Locale.forLanguageTag(tag) else systemLocale
  }
}
