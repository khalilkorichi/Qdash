package com.qdash.domain.usecase.categories

import com.qdash.domain.model.Category
import com.qdash.domain.model.CategoryType
import com.qdash.domain.repository.CategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Use case to retrieve parent categories with their nested subcategories hierarchy.
 * Executes on Dispatchers.Default.
 */
class GetCategoriesWithSubcategoriesUseCase(
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(type: CategoryType? = null): Flow<List<Category>> {
        return categoryRepository.getAllCategories().map { allCategories ->
            val parents = allCategories.filter { it.parentId == null && (type == null || it.type == type) }
            val subcategoriesByParent = allCategories.filter { it.parentId != null }.groupBy { it.parentId }

            parents.map { parent ->
                val subs = subcategoriesByParent[parent.id] ?: emptyList()
                parent.copy(subcategories = subs.sortedBy { it.sortOrder })
            }.sortedBy { it.sortOrder }
        }.flowOn(Dispatchers.Default)
    }
}
