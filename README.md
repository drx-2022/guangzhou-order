test product card by this Main class:

package com.example.guangzhouorder;

import com.example.guangzhouorder.entity.Category;
import com.example.guangzhouorder.entity.ProductCard;
import com.example.guangzhouorder.repository.CategoryRepository;
import com.example.guangzhouorder.repository.ProductCardRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

@SpringBootApplication
public class GuangzhouOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuangzhouOrderApplication.class, args);
    }
    @Bean
    CommandLineRunner seedData(CategoryRepository catRepo, ProductCardRepository cardRepo) {
        return args -> {
            if (cardRepo.count() == 0) {
                Category cat = catRepo.save(Category.builder().name("Electronics").build());
                cardRepo.save(ProductCard.builder()
                        .cardDna("{\"name\":\"Test Watch\",\"material\":\"Titanium\",\"stock\":\"50\"}")
                        .isPublic(true)
                        .displayPrice(new BigDecimal("1500000"))
                        .category(cat)
                        .build());
            }
        };
    }
}
