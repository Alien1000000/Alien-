package com.alienavi.aliennews

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewsUiState(
    val articles: List<Article> = emptyList(),
    val savedIds: Set<String> = emptySet(),
    val language: String = "he",
    val tab: AppTab = AppTab.HOME,
    val query: String = "",
    val loading: Boolean = true,
    val offline: Boolean = false,
    val loadFailed: Boolean = false,
    val notifications: Boolean = false,
)

class NewsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NewsRepository(application)
    private val _state = MutableStateFlow(
        NewsUiState(
            language = repository.language(),
            savedIds = repository.savedIds(),
            notifications = repository.notificationsEnabled(),
        ),
    )
    val state: StateFlow<NewsUiState> = _state

    init { refresh() }

    fun refresh() {
        val language = _state.value.language
        val cached = repository.cached(language)
        if (cached.isNotEmpty()) _state.update { it.copy(articles = cached, loading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.fetch(language) }
                .onSuccess { items ->
                    _state.update {
                        if (it.language == language) it.copy(articles = items, loading = false, offline = false, loadFailed = false)
                        else it
                    }
                }
                .onFailure {
                    _state.update {
                        if (it.language != language) it else it.copy(
                            articles = cached,
                            loading = false,
                            offline = cached.isNotEmpty(),
                            loadFailed = cached.isEmpty(),
                        )
                    }
                }
        }
    }

    fun setTab(tab: AppTab) = _state.update { it.copy(tab = tab) }
    fun setQuery(query: String) = _state.update { it.copy(query = query) }

    fun setLanguage(language: String) {
        if (language == _state.value.language) return
        repository.setLanguage(language)
        _state.update {
            it.copy(
                language = language,
                articles = repository.cached(language),
                query = "",
                loading = true,
                offline = false,
                loadFailed = false,
            )
        }
        refresh()
    }

    fun toggleSaved(article: Article) {
        repository.toggleSaved(article)
        _state.update { it.copy(savedIds = repository.savedIds()) }
    }

    fun setNotifications(enabled: Boolean) {
        repository.setNotificationsEnabled(enabled)
        _state.update { it.copy(notifications = enabled) }
    }
}
