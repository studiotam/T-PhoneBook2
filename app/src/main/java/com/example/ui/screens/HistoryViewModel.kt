package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.CallLogRepository
import com.example.domain.model.CallRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: CallLogRepository) : ViewModel() {

    val outgoingCalls: StateFlow<List<CallRecord>> = repository.getOutgoingCalls()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomingCalls: StateFlow<List<CallRecord>> = repository.getIncomingCalls()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTabIndex = MutableStateFlow(0) // 0: 発信履歴, 1: 着信履歴
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        repository.refresh()
    }

    fun setSelectedTabIndex(index: Int) {
        _selectedTabIndex.value = index
        refresh()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearHistory(isOutgoingTab: Boolean) {
        viewModelScope.launch {
            if (isOutgoingTab) {
                repository.clearOutgoingHistory()
            } else {
                repository.clearIncomingHistory()
            }
            refresh()
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllLocalHistory()
            refresh()
        }
    }
}

class HistoryViewModelFactory(private val repository: CallLogRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
