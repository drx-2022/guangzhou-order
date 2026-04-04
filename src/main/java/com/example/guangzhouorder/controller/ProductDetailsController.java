package com.example.guangzhouorder.controller;

import com.example.guangzhouorder.dto.ProductCardDto;
import com.example.guangzhouorder.repository.ProductCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class ProductDetailsController {

    private final ProductCardRepository productCardRepository;

    @GetMapping("/products/{id}")
    public String productDetails(@PathVariable Long id, Model model) {
        ProductCardDto card = productCardRepository.findById(id)
                .filter(c -> c.isPublic())
                .map(ProductCardDto::new)
                .orElseThrow(() -> new IllegalArgumentException("Product not found or not public"));
        model.addAttribute("card", card);
        return "product_details";
    }
}
