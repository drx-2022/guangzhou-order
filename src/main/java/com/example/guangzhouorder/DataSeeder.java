package com.example.guangzhouorder;

import com.example.guangzhouorder.entity.Category;
import com.example.guangzhouorder.entity.BaseProduct;
import com.example.guangzhouorder.entity.Order;
import com.example.guangzhouorder.entity.ProductCard;
import com.example.guangzhouorder.entity.User;
import com.example.guangzhouorder.repository.BaseProductRepository;
import com.example.guangzhouorder.repository.CategoryRepository;
import com.example.guangzhouorder.repository.OrderRepository;
import com.example.guangzhouorder.repository.ProductCardRepository;
import com.example.guangzhouorder.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
@org.springframework.core.annotation.Order(1)
public class DataSeeder implements ApplicationRunner {

    private static final int TARGET_PRICE_HISTORY_POINTS = 120;

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BaseProductRepository baseProductRepository;
    private final OrderRepository orderRepository;
    private final ProductCardRepository productCardRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    public DataSeeder(UserRepository userRepository, CategoryRepository categoryRepository, BaseProductRepository baseProductRepository,
                      OrderRepository orderRepository, ProductCardRepository productCardRepository,
                      BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.baseProductRepository = baseProductRepository;
        this.orderRepository = orderRepository;
        this.productCardRepository = productCardRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
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

            User affiliate = User.builder()
                    .name("Affiliate 1")
                    .email("affiliate1@gmail.com")
                    .hashedPassword("password")
                    .role("AFFILIATE")
                    .build();
            userRepository.save(affiliate);
        }

        if (!userRepository.existsByEmail("affiliate1@gmail.com")) {
            User affiliate = User.builder()
                    .name("Affiliate 1")
                    .email("affiliate1@gmail.com")
                    .hashedPassword("password")
                    .role("AFFILIATE")
                    .build();
            userRepository.save(affiliate);
        }

        // Seed categories if table is empty
        if (categoryRepository.count() == 0) {
            List<String> categoryNames = List.of("Leather Goods", "Electronics", "Footwear", "Textiles", "Hardware");
            for (String name : categoryNames) {
                Category category = Category.builder().name(name).build();
                categoryRepository.save(category);
            }
        }

        // Seed base products for grouped price charts
        if (baseProductRepository.count() == 0) {
            Category leatherGoods = categoryRepository.findAll().stream()
                    .filter(c -> "Leather Goods".equals(c.getName()))
                    .findFirst()
                    .orElseGet(() -> categoryRepository.save(Category.builder().name("Leather Goods").build()));

            baseProductRepository.save(BaseProduct.builder()
                    .name("Leather Bag")
                    .description("Base product for leather bag variants and customizations")
                    .thumbnailUrl("https://images.unsplash.com/photo-1548036328-c9fa89d128fa")
                    .category(leatherGoods)
                    .isActive(true)
                    .build());
        }

        seedPriceHistoryData();
    }

    private void seedPriceHistoryData() {
        User customer = userRepository.findByEmail("customer1@gmail.com").orElseThrow();
        Category leatherGoods = categoryRepository.findAll().stream()
                .filter(c -> "Leather Goods".equals(c.getName()))
                .findFirst()
                .orElseGet(() -> categoryRepository.save(Category.builder().name("Leather Goods").build()));

        BaseProduct leatherBag = baseProductRepository.findByName("Leather Bag")
                .orElseGet(() -> baseProductRepository.save(BaseProduct.builder()
                        .name("Leather Bag")
                        .description("Base product for leather bag variants and customizations")
                        .thumbnailUrl("https://images.unsplash.com/photo-1548036328-c9fa89d128fa")
                        .category(leatherGoods)
                        .isActive(true)
                        .build()));

        int existingCount = productCardRepository.findByBaseProduct(leatherBag).size();
        if (existingCount >= TARGET_PRICE_HISTORY_POINTS) {
            return;
        }

        List<String> materials = List.of("Genuine Leather", "Top Grain Leather", "PU Leather", "Waxed Leather");
        List<String> colors = List.of("Brown", "Black", "Tan", "Olive", "Navy");
        List<String> images = List.of(
                "https://images.unsplash.com/photo-1548036328-c9fa89d128fa",
                "https://images.unsplash.com/photo-1594223274512-ad4803739b7c",
                "https://images.unsplash.com/photo-1584917865442-de89df76afd3",
                "https://images.unsplash.com/photo-1622560480605-d83c853bc5c3"
        );

        for (int i = existingCount; i < TARGET_PRICE_HISTORY_POINTS; i++) {
            String material = materials.get(i % materials.size());
            String color = colors.get(i % colors.size());
            String image = images.get(i % images.size());
            int quantity = 60 + (i % 9) * 20;

            BigDecimal price = buildHistoricalPrice(i);
            BigDecimal deposit = price.multiply(new BigDecimal("0.3")).setScale(2, RoundingMode.HALF_UP);

            String specs = String.format(
                    "{\"name\":\"Leather Bag\",\"material\":\"%s\",\"color\":\"%s\",\"quantity\":%d,\"imageUrl\":\"%s\",\"configuration\":\"Factory Batch %02d\"}",
                    material, color, quantity, image, (i % 15) + 1
            );

            Order order = orderRepository.save(Order.builder()
                    .customer(customer)
                    .structuredSpecs(specs)
                    .status("DONE")
                    .paymentStatus("DONE")
                    .depositAmount(deposit)
                    .finalPrice(price)
                    .build());

            ProductCard card = productCardRepository.save(ProductCard.builder()
                    .cardName("Leather Bag Variant " + (i + 1))
                    .sourceOrder(order)
                    .category(leatherGoods)
                    .baseProduct(leatherBag)
                    .cardDna(specs)
                    .isPublic(true)
                    .displayPrice(price)
                    .build());

            LocalDateTime historicalTime = LocalDateTime.now().minusDays(TARGET_PRICE_HISTORY_POINTS - i);
            backdateOrderAndCard(order.getOrderId(), card.getProductCardId(), historicalTime);
        }
    }

    private BigDecimal buildHistoricalPrice(int index) {
        int randomSwing = ThreadLocalRandom.current().nextInt(-120000, 120001);
        int trend = index * 9000;
        int seasonal = (index % 10) * 18000;
        int value = 1450000 + trend + seasonal + randomSwing;
        if (value < 900000) {
            value = 900000;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private void backdateOrderAndCard(Long orderId, Long productCardId, LocalDateTime time) {
        Timestamp ts = Timestamp.valueOf(time);
        entityManager.createNativeQuery("update orders set created_at = :ts, updated_at = :ts where order_id = :id")
                .setParameter("ts", ts)
                .setParameter("id", orderId)
                .executeUpdate();

        entityManager.createNativeQuery("update product_cards set created_at = :ts, updated_at = :ts where product_card_id = :id")
                .setParameter("ts", ts)
                .setParameter("id", productCardId)
                .executeUpdate();
    }
}
