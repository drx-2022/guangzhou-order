package com.example.guangzhouorder.controller;

import com.example.guangzhouorder.entity.Category;
import com.example.guangzhouorder.repository.CategoryRepository;
import com.example.guangzhouorder.service.PriceChartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PriceChartController {

    private final CategoryRepository categoryRepository;
    private final PriceChartService priceChartService;

    @GetMapping("/price-chart")
    public String showPriceChart(@RequestParam(required = false) Long categoryId, Model model) {
        List<Category> categories = categoryRepository.findAll();
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategoryId", categoryId);
        return "interactive_price_chart";
    }

    @GetMapping("/api/price-chart/data")
    @ResponseBody
    public List<PriceChartService.PriceChartDataPoint> getChartData(@RequestParam(required = false) Long categoryId) {
        return priceChartService.getChartData(categoryId);
    }
}
