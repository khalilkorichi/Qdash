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
    val allCategories: List<Category> = emptyList(),
    val rootCategories: List<Category> = emptyList(),
    val subcategoryCounts: Map<Long, Int> = emptyMap(),
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

    private val _selectedParentId = MutableStateFlow<Long?>(null)

    init {
        observeCategories()
    }

    private fun observeCategories() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                combine(
                    categoryRepository.getAllCategories(),
                    _selectedParentId
                ) { allCats, selectedParentId ->
                    val roots = allCats.filter { it.parentId == null }.sortedBy { it.sortOrder }
                    val counts = allCats
                        .filter { it.parentId != null }
                        .groupingBy { it.parentId!! }
                        .eachCount()
                    val selectedParent = selectedParentId?.let { id -> roots.find { it.id == id } }
                    val subs = if (selectedParentId != null) {
                        allCats.filter { it.parentId == selectedParentId }.sortedBy { it.sortOrder }
                    } else {
                        emptyList()
                    }
                    Triple(Pair(allCats, roots), Pair(counts, selectedParent), subs)
                }.collect { (catsPair, metaPair, subs) ->
                    val (allCats, roots) = catsPair
                    val (counts, selectedParent) = metaPair
                    _uiState.update {
                        it.copy(
                            allCategories = allCats,
                            rootCategories = roots,
                            subcategoryCounts = counts,
                            selectedParent = selectedParent,
                            subcategories = subs,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    fun selectParent(category: Category?) {
        _selectedParentId.value = category?.id
        if (category == null) {
            _uiState.update { it.copy(selectedParent = null, subcategories = emptyList()) }
        } else {
            val subs = _uiState.value.allCategories
                .filter { it.parentId == category.id }
                .sortedBy { it.sortOrder }
            _uiState.update { it.copy(selectedParent = category, subcategories = subs) }
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

    fun moveCategory(categoryId: Long, newParentId: Long?) {
        viewModelScope.launch {
            try {
                categoryRepository.moveCategory(categoryId, newParentId)
                // If a parent was selected, re-trigger its subcategories load
                _uiState.value.selectedParent?.let { parent ->
                    selectParent(parent)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
