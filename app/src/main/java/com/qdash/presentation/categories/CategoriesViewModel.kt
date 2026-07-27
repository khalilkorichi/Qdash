package com.qdash.presentation.categories

import androidx.compose.runtime.Immutable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.Category
import com.qdash.domain.model.CategoryType
import com.qdash.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Immutable
data class CategoriesUiState(
    val rootCategories: List<Category> = emptyList(),
    val selectedParent: Category? = null,
    val subcategories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CategoriesViewModel(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    private var subcategoriesJob: kotlinx.coroutines.Job? = null

    init {
        loadRootCategories()
    }

    private fun loadRootCategories() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                categoryRepository.getRootCategories().collect { list ->
                    _uiState.update { it.copy(rootCategories = list, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    fun selectParent(category: Category?) {
        subcategoriesJob?.cancel()
        if (category == null) {
            _uiState.update { it.copy(selectedParent = null, subcategories = emptyList()) }
            return
        }
        _uiState.update { it.copy(selectedParent = category) }
        subcategoriesJob = viewModelScope.launch {
            try {
                categoryRepository.getSubcategories(category.id).collect { subs ->
                    _uiState.update { it.copy(subcategories = subs) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }

    fun addCategory(
        name: String,
        type: CategoryType,
        icon: String,
        color: String,
        parentId: Long? = null
    ) {
        viewModelScope.launch {
            try {
                val category = Category(
                    name = name,
                    type = type,
                    icon = icon,
                    color = color,
                    parentId = parentId,
                    isSystem = false
                )
                categoryRepository.insertCategory(category)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            try {
                categoryRepository.updateCategory(category)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            try {
                val txCount = categoryRepository.getTransactionCountForCategory(category.id)
                if (txCount > 0) {
                    _uiState.update {
                        it.copy(errorMessage = "لا يمكن حذف الفئة لأنها تحتوي على معاملات مرتبطة")
                    }
                } else {
                    categoryRepository.deleteSubcategoriesForParent(category.id)
                    categoryRepository.deleteCategory(category)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }

    fun mergeCategories(sourceCategoryId: Long, targetCategoryId: Long) {
        viewModelScope.launch {
            try {
                categoryRepository.mergeCategories(sourceCategoryId, targetCategoryId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
