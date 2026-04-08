package com.example.guangzhouorder.controller;

import com.example.guangzhouorder.dto.ProductCardDto;
import com.example.guangzhouorder.entity.BaseProduct;
import com.example.guangzhouorder.entity.ProductCard;
import com.example.guangzhouorder.entity.ReferralClick;
import com.example.guangzhouorder.entity.User;
import com.example.guangzhouorder.repository.ProductCardRepository;
import com.example.guangzhouorder.repository.ReferralClickRepository;
import com.example.guangzhouorder.repository.UserRepository;
import com.example.guangzhouorder.service.PriceChartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;

@Controller
@RequiredArgsConstructor
public class ProductDetailsController {

    private final ProductCardRepository productCardRepository;
    private final PriceChartService priceChartService;
    private final UserRepository userRepository;
    private final ReferralClickRepository referralClickRepository;

    @GetMapping("/products/{id}")
    public String productDetails(@PathVariable Long id,
                                 @RequestParam(required = false) Long ref,
                                 Model model) {
        ProductCard card = productCardRepository.findById(id)
                .filter(c -> c.isPublic())
                .orElseThrow(() -> new IllegalArgumentException("Product not found or not public"));

        if (ref != null) {
            User affiliate = userRepository.findById(ref)
                    .filter(u -> "AFFILIATE".equals(u.getRole()))
                    .orElse(null);

            if (affiliate != null) {
                referralClickRepository.save(ReferralClick.builder()
                        .affiliate(affiliate)
                        .productCard(card)
                        .build());
            }
        }

        ProductCardDto cardDto = new ProductCardDto(card);
        model.addAttribute("card", cardDto);

        BaseProduct baseProduct = card.getBaseProduct();

        if (baseProduct != null) {
            model.addAttribute("chartData", priceChartService.getChartDataByBaseProduct(baseProduct.getBaseProductId()));
            model.addAttribute("baseProductName", baseProduct.getName());
        } else {
            model.addAttribute("chartData", Collections.emptyList());
            model.addAttribute("baseProductName", cardDto.getName());
        }

        return "product_details";
    }
}
