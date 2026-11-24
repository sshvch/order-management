package ru.orderhub.order_management.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import ru.orderhub.order_management.entity.Category;
import ru.orderhub.order_management.entity.Product;
import ru.orderhub.order_management.entity.Role;
import ru.orderhub.order_management.entity.User;
import ru.orderhub.order_management.repository.CartItemRepository;
import ru.orderhub.order_management.repository.CartRepository;
import ru.orderhub.order_management.repository.CategoryRepository;
import ru.orderhub.order_management.repository.OrderRepository;
import ru.orderhub.order_management.repository.ProductRepository;
import ru.orderhub.order_management.repository.UserRepository;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderManagementApiIntegrationTest {

    private static final String PASSWORD = "secret123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerReturnsTokenAndMeReturnsCurrentUser() throws Exception {
        String email = "buyer@test.local";

        String token = register(email);

        assertThat(token).isNotBlank();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void protectedEndpointWithoutTokenReturns401InUnifiedFormat() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Требуется авторизация"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void userCannotOpenAdminEndpoint() throws Exception {
        String token = register("simple@test.local");

        mockMvc.perform(get("/api/orders/all")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Недостаточно прав"));
    }

    @Test
    void userOrderDecreasesStockAndClearsCart() throws Exception {
        String adminToken = createAdminAndLogin("admin@test.local");
        long categoryId = createCategory(adminToken, "Электроника");
        long productId = createProduct(adminToken, "Ноутбук", "100.00", 5, categoryId);

        String userEmail = "buyer@test.local";
        String userToken = register(userEmail);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": %d, "quantity": 2}
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2));

        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andReturn();

        double total = ((Number) JsonPath.read(
                orderResult.getResponse().getContentAsString(), "$.totalAmount")).doubleValue();
        assertThat(total).isEqualTo(200.0);

        assertThat(stockOf(productId)).isEqualTo(3);
        assertThat(cartRepository.findByUserEmail(userEmail).orElseThrow().getItems()).isEmpty();
        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    void orderRollsBackWhenProductWasAlreadySoldOut() throws Exception {
        String adminToken = createAdminAndLogin("admin@test.local");
        long categoryId = createCategory(adminToken, "Электроника");
        long productId = createProduct(adminToken, "Последний ноутбук", "100.00", 1, categoryId);

        String userEmail = "buyer@test.local";
        String userToken = register(userEmail);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": %d, "quantity": 1}
                                """.formatted(productId)))
                .andExpect(status().isOk());

        Product product = productRepository.findById(productId).orElseThrow();
        product.setStockQuantity(0);
        productRepository.save(product);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Недостаточно товара")));

        assertThat(stockOf(productId)).isZero();
        assertThat(orderRepository.count()).isZero();
        assertThat(cartRepository.findByUserEmail(userEmail).orElseThrow().getItems())
                .hasSize(1);
    }

    @Test
    @Transactional
    void decreaseStockIsAtomicAndNeverGoesBelowZero() {
        Category category = new Category();
        category.setName("Тестовая категория");
        categoryRepository.save(category);

        Product product = new Product();
        product.setName("Единственный товар");
        product.setPrice(new BigDecimal("10.00"));
        product.setStockQuantity(1);
        product.setActive(true);
        product.setCategory(category);
        productRepository.save(product);

        int first = productRepository.decreaseStock(product.getId(), 1);
        int second = productRepository.decreaseStock(product.getId(), 1);

        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
        assertThat(productRepository.findStockQuantityById(product.getId())).isZero();
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private String createAdminAndLogin(String email) throws Exception {
        User admin = new User();
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(PASSWORD));
        admin.setRole(Role.ADMIN);
        userRepository.saveAndFlush(admin);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private long createCategory(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();

        return ((Number) JsonPath.read(
                result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long createProduct(String token, String name, String price, int stock, long categoryId)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "description": "Товар для теста",
                                  "price": %s,
                                  "stockQuantity": %d,
                                  "categoryId": %d
                                }
                                """.formatted(name, price, stock, categoryId)))
                .andExpect(status().isCreated())
                .andReturn();

        return ((Number) JsonPath.read(
                result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private int stockOf(long productId) {
        return productRepository.findById(productId).orElseThrow().getStockQuantity();
    }
}
