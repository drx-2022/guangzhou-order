package com.example.guangzhouorder.controller;

import com.example.guangzhouorder.dto.ProductCardDto;
import com.example.guangzhouorder.entity.Category;
import com.example.guangzhouorder.entity.ProductCard;
import com.example.guangzhouorder.repository.CategoryRepository;
import com.example.guangzhouorder.repository.ProductCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class CatalogController {

    private final ProductCardRepository productCardRepository;
    private final CategoryRepository categoryRepository;

    @GetMapping("/catalog")
    public String catalog(@RequestParam(required = false) Long categoryId, Model model) {
        List<ProductCard> cards;

        if (categoryId != null) {
            Optional<Category> category = categoryRepository.findById(categoryId);
            cards = category
                    .map(productCardRepository::findByIsPublicTrueAndCategoryOrderByCreatedAtDesc)
                    .orElseGet(productCardRepository::findByIsPublicTrueOrderByCreatedAtDesc);
        } else {
            cards = productCardRepository.findByIsPublicTrueOrderByCreatedAtDesc();
        }

        List<ProductCardDto> cardDtos = cards.stream().map(ProductCardDto::new).toList();
        List<Category> categories = categoryRepository.findAll();

        model.addAttribute("cards", cardDtos);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategoryId", categoryId);

        return "community_catalog";
    }
}
