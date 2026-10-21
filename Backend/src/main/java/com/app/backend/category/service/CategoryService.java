package com.app.backend.category.service;

import com.app.backend.category.Category;
import com.app.backend.category.CategoryRepository;
import com.app.backend.category.dto.CategoryResponse;
import com.app.backend.category.dto.CreateCategoryRequest;
import com.app.backend.category.dto.UpdateCategoryRequest;
import com.app.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> listAll() {
        return categoryRepository.findAll(Sort.by(Sort.Order.asc("name"))).stream()
                .map(CategoryService::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        String name = normalizeName(request.name());
        if (categoryRepository.existsByName(name)) {
            throw BusinessException.conflict("CATEGORY_NAME_ALREADY_EXISTS", "Category name already exists");
        }
        Category category = new Category();
        category.setName(name);
        category.setDescription(request.description());
        try {
            categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException e) {
            throw BusinessException.conflict("CATEGORY_NAME_ALREADY_EXISTS", "Category name already exists");
        }
        return toResponse(category);
    }

    @Transactional
    public CategoryResponse update(Long id, UpdateCategoryRequest request) {
        Category category = findById(id);
        String name = normalizeName(request.name());
        if (!category.getName().equals(name) && categoryRepository.existsByName(name)) {
            throw BusinessException.conflict("CATEGORY_NAME_ALREADY_EXISTS", "Category name already exists");
        }
        category.setName(name);
        category.setDescription(request.description());
        try {
            categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException e) {
            throw BusinessException.conflict("CATEGORY_NAME_ALREADY_EXISTS", "Category name already exists");
        }
        return toResponse(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = findById(id);
        if (categoryRepository.countProducts(id) > 0) {
            throw BusinessException.conflict("CATEGORY_HAS_PRODUCTS",
                    "Category still has products; remove or reassign them first");
        }
        categoryRepository.delete(category);
    }

    private Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("CATEGORY_NOT_FOUND", "Category not found"));
    }

    private static String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }
}
