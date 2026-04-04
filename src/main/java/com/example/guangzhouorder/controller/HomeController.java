package com.example.guangzhouorder.controller;

import com.example.guangzhouorder.entity.ProductCard;
import com.example.guangzhouorder.repository.ProductCardRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private final ProductCardRepository productCardRepository;

    public HomeController(ProductCardRepository productCardRepository) {
        this.productCardRepository = productCardRepository;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<ProductCard> featuredProducts = productCardRepository.findTop6ByIsPublicTrue();
        model.addAttribute("featuredProducts", featuredProducts);
        model.addAttribute("pageTitle", "Guangzhou Direct | Source from China");
        return "home";
    }
}
