package com.apexagro.reference.internal.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apexagro.common.i18n.Language;
import com.apexagro.reference.internal.dto.response.CategoryResponse;
import com.apexagro.reference.internal.entity.Category;
import com.apexagro.reference.internal.repository.CategoryRepository;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Cacheable(cacheNames = "categories", key = "#language")
    public List<CategoryResponse> getCategories(Language language) {
        // 1. Получаем все активные категории за один запрос
        List<Category> allCategories = categoryRepository.findAllByActiveTrueOrderBySortOrderAscNameRuAsc();

        // 2. Группируем дочерние элементы по parentId
        Map<UUID, List<Category>> childrenByParentId = allCategories.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        // 3. Собираем дерево от корней (у кого parent == null)
        return allCategories.stream()
                .filter(c -> c.getParent() == null)
                .map(root -> buildNode(root, childrenByParentId, language))
                .toList();
    }

    private CategoryResponse buildNode(Category category, Map<UUID, List<Category>> childrenMap, Language language) {
        String name = language == Language.KY ? category.getNameKy() : category.getNameRu();
        UUID parentId = category.getParent() != null ? category.getParent().getId() : null;

        List<Category> children = childrenMap.getOrDefault(category.getId(), Collections.emptyList());
        List<CategoryResponse> childResponses = children.stream()
                .map(child -> buildNode(child, childrenMap, language))
                .toList();

        return new CategoryResponse(
                category.getId(),
                parentId,
                category.getLevel(),
                category.getCode(),
                name,
                category.getKind(),
                category.getSortOrder(),
                category.isActive(),
                childResponses
        );
    }
}