package com.qdash.presentation.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.TransactionTemplate
import com.qdash.domain.usecase.templates.*
import com.qdash.domain.repository.CategoryRepository
import com.qdash.domain.repository.AccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class TemplatesEvent {
    data class NavigateToPreFill(
        val draftJson: String,
        val transactionType: String = "EXPENSE"
    ) : TemplatesEvent()
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

    fun onCreateTemplate(template: TransactionTemplate, onSaved: () -> Unit = {}) {
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
                onSaved()
            }
        }
    }

    fun forceCreateTemplate(template: TransactionTemplate, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            createTemplateUseCase(template)
            _uiState.update { it.copy(duplicateWarningTemplate = null) }
            onSaved()
        }
    }

    fun clearDuplicateWarning() {
        _uiState.update { it.copy(duplicateWarningTemplate = null) }
    }

    fun onUpdateTemplate(template: TransactionTemplate, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            updateTemplateUseCase(template)
            onSaved()
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
                val json = encodeDraftToJson(draft)
                _eventChannel.send(
                    TemplatesEvent.NavigateToPreFill(
                        draftJson = json,
                        transactionType = draft.type.name
                    )
                )
            }
        }
    }

    private fun encodeDraftToJson(draft: com.qdash.domain.model.TransactionDraft): String {
        return org.json.JSONObject().apply {
            put("amount", draft.amount)
            put("type", draft.type.name)
            put("categoryId", draft.categoryId ?: org.json.JSONObject.NULL)
            put("subcategoryId", draft.subcategoryId ?: org.json.JSONObject.NULL)
            put("accountId", draft.accountId)
            put("targetAccountId", draft.targetAccountId ?: org.json.JSONObject.NULL)
            put("notes", draft.notes ?: org.json.JSONObject.NULL)
            put("templateId", draft.templateId ?: org.json.JSONObject.NULL)
        }.toString()
    }
}
