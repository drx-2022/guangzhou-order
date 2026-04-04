package com.example.guangzhouorder;

import com.example.guangzhouorder.entity.Category;
import com.example.guangzhouorder.entity.Order;
import com.example.guangzhouorder.entity.ProductCard;
import com.example.guangzhouorder.entity.User;
import com.example.guangzhouorder.repository.CategoryRepository;
import com.example.guangzhouorder.repository.OrderRepository;
import com.example.guangzhouorder.repository.ProductCardRepository;
import com.example.guangzhouorder.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@org.springframework.core.annotation.Order(1)
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final ProductCardRepository productCardRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, CategoryRepository categoryRepository,
                      OrderRepository orderRepository, ProductCardRepository productCardRepository,
                      BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.orderRepository = orderRepository;
        this.productCardRepository = productCardRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Seed users if table is empty
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .name("Admin")
                    .email("admin@guangzhou.com")
                    .hashedPassword(passwordEncoder.encode("password"))
                    .role("ADMIN")
                    .build();
            userRepository.save(admin);

            User customer1 = User.builder()
                    .name("Customer 1")
                    .email("customer1@gmail.com")
                    .hashedPassword("password")  // Not hashed as per request, but set to satisfy entity constraint
                    .role("CUSTOMER")
                    .build();
            userRepository.save(customer1);

            User customer2 = User.builder()
                    .name("Customer 2")
                    .email("customer2@gmail.com")
                    .hashedPassword("password")  // Not hashed as per request, but set to satisfy entity constraint
                    .role("CUSTOMER")
                    .build();
            userRepository.save(customer2);
        }

        // Seed categories if table is empty
        if (categoryRepository.count() == 0) {
            List<String> categoryNames = List.of("Leather Goods", "Electronics", "Footwear", "Textiles", "Hardware");
            for (String name : categoryNames) {
                Category category = Category.builder().name(name).build();
                categoryRepository.save(category);
            }
        }

        // Seed orders if table is empty (linked to customer1)
        List<Order> seededOrders = List.of();
        if (orderRepository.count() == 0) {
            User customer1 = userRepository.findByEmail("customer1@gmail.com").orElseThrow();
            List<String> specs = List.of(
                    "{\"material\":\"leather\",\"color\":\"brown\",\"quantity\":100}",
                    "{\"material\":\"plastic\",\"color\":\"blue\",\"quantity\":200}",
                    "{\"material\":\"metal\",\"color\":\"silver\",\"quantity\":50}"
            );
            seededOrders = specs.stream().map(spec -> Order.builder()
                    .customer(customer1)
                    .structuredSpecs(spec)
                    .status("DONE")
                    .paymentStatus("DONE")
                    .depositAmount(new BigDecimal("500.00"))
                    .finalPrice(new BigDecimal("2000.00"))
                    .build()).toList();
            orderRepository.saveAll(seededOrders);
        }

        // Seed product cards if table is empty (one per order)
        if (productCardRepository.count() == 0 && !seededOrders.isEmpty()) {
            for (Order order : seededOrders) {
                ProductCard card = ProductCard.builder()
                        .sourceOrder(order)
                        .cardDna(order.getStructuredSpecs())
                        .isPublic(true)
                        .displayPrice(order.getFinalPrice())
                        .build();
                productCardRepository.save(card);
            }
        }
    }
}
