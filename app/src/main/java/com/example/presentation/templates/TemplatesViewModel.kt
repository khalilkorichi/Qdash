package com.example.presentation.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.TransactionTemplate
import com.example.domain.usecase.templates.*
import com.example.domain.repository.CategoryRepository
import com.example.domain.repository.AccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class TemplatesEvent {
    data class NavigateToPreFill(val draftJson: String) : TemplatesEvent()
    object NavigateBack : TemplatesEvent()
}

class TemplatesViewModel(
    private val getAllTemplatesUseCase: GetAllTemplatesUseCase,
    private val getPinnedTemplatesUseCase: GetPinnedTemplatesUseCase,
    private val getFrequentTemplatesUseCase: GetFrequentTemplatesUseCase,
    private val createTemplateUseCase: CreateTemplateUseCase,
    private val updateTemplateUseCase: UpdateTemplateUseCase,
    private val deleteTemplateUseCase: DeleteTemplateUseCase,
    private val useTemplateUseCase: UseTemplateUseCase,
    private val togglePinTemplateUseCase: TogglePinTemplateUseCase,
    private val searchTemplatesUseCase: SearchTemplatesUseCase,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplatesUiState())
    val uiState: StateFlow<TemplatesUiState> = _uiState.asStateFlow()

    private val _eventChannel = Channel<TemplatesEvent>()
    val events = _eventChannel.receiveAsFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        loadStaticDependencies()
        observeTemplatesStreams()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            kotlinx.coroutines.delay(800)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun loadStaticDependencies() {
        viewModelScope.launch {
            combine(
                categoryRepository.getAllCategories(),
                accountRepository.getAllAccounts()
            ) { cats, accs ->
                _uiState.update { it.copy(categories = cats, accounts = accs) }
            }.collect()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeTemplatesStreams() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            launch {
                getAllTemplatesUseCase().collect { list ->
                    _uiState.update { it.copy(templates = list, isLoading = false) }
                }
            }

            launch {
                getPinnedTemplatesUseCase().collect { list ->
                    _uiState.update { it.copy(pinnedTemplates = list) }
                }
            }

            launch {
                getFrequentTemplatesUseCase().collect { list ->
                    _uiState.update { it.copy(frequentTemplates = list) }
                }
            }

            // Debounced search query flow
            launch {
                _searchQuery
                    .debounce(300L)
                    .flatMapLatest { query ->
                        if (query.isBlank()) {
                            flowOf(emptyList())
                        } else {
                            searchTemplatesUseCase(query)
                        }
                    }
                    .flowOn(Dispatchers.Default)
                    .collect { results ->
                        _uiState.update { it.copy(searchResults = results, isSearching = _searchQuery.value.isNotBlank()) }
                    }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onCreateTemplate(template: TransactionTemplate) {
        viewModelScope.launch {
            // Check for identical template duplicate
            val isDuplicate = _uiState.value.templates.any {
                it.name.trim().lowercase() == template.name.trim().lowercase() ||
                (it.amount == template.amount &&
                 it.transactionType == template.transactionType &&
                 it.accountId == template.accountId &&
                 it.categoryId == template.categoryId)
            }
            
            if (isDuplicate && _uiState.value.duplicateWarningTemplate == null) {
                _uiState.update { it.copy(duplicateWarningTemplate = template) }
            } else {
                createTemplateUseCase(template)
                _uiState.update { it.copy(duplicateWarningTemplate = null) }
            }
        }
    }

    fun forceCreateTemplate(template: TransactionTemplate) {
        viewModelScope.launch {
            createTemplateUseCase(template)
            _uiState.update { it.copy(duplicateWarningTemplate = null) }
        }
    }

    fun clearDuplicateWarning() {
        _uiState.update { it.copy(duplicateWarningTemplate = null) }
    }

    fun onUpdateTemplate(template: TransactionTemplate) {
        viewModelScope.launch {
            updateTemplateUseCase(template)
        }
    }

    fun onDeleteTemplate(id: Long) {
        viewModelScope.launch {
            deleteTemplateUseCase(id)
            _uiState.update { it.copy(showDeleteConfirmation = null) }
        }
    }

    fun onTogglePin(id: Long, isPinned: Boolean) {
        viewModelScope.launch {
            togglePinTemplateUseCase(id, isPinned)
        }
    }

    fun showDeleteConfirmation(template: TransactionTemplate?) {
        _uiState.update { it.copy(showDeleteConfirmation = template) }
    }

    fun onUseTemplate(templateId: Long) {
        viewModelScope.launch {
            val draft = useTemplateUseCase(templateId)
            if (draft != null) {
                // Serialize draft to JSON string to pass safely via Navigation Compose route args
                val json = encodeDraftToJson(draft)
                _eventChannel.send(TemplatesEvent.NavigateToPreFill(json))
            }
        }
    }

    private fun encodeDraftToJson(draft: com.example.domain.model.TransactionDraft): String {
        // Quick simple custom JSON serializer to keep dependency clean and fast
        return """
            {
                "amount": ${draft.amount},
                "type": "${draft.type.name}",
                "categoryId": ${draft.categoryId ?: "null"},
                "subcategoryId": ${draft.subcategoryId ?: "null"},
                "accountId": ${draft.accountId},
                "targetAccountId": ${draft.targetAccountId ?: "null"},
                "notes": "${draft.notes?.replace("\"", "\\\"") ?: ""}",
                "templateId": ${draft.templateId ?: "null"}
            }
        """.trimIndent().replace("\n", "").replace(" ", "")
    }
}
