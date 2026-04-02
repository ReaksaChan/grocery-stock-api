package com.reaksa.e_wingshop_api.config;

import com.reaksa.e_wingshop_api.entity.*;
import com.reaksa.e_wingshop_api.enums.RoleName;
import com.reaksa.e_wingshop_api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Programmatic seed — runs only on "dev" or "test" profile.
 * Skips gracefully if data already exists.
 * Execution order = 2 (after DataInitializer which seeds roles at order 1).
 */
@Component
@Profile({"dev", "test"})
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository  inventoryRepository;
    private final PasswordEncoder      passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 1) {
            log.info("DataSeeder: data already present, skipping.");
            return;
        }
        log.info("DataSeeder: seeding demo data...");

        List<User>     users      = seedUsers();
        List<Branch>   branches   = seedBranches();
        assignManagerBranches(users, branches);
        List<Category> categories = seedCategories();
        List<Product>  products   = seedProducts(categories);
        seedInventories(branches, products);

        log.info("DataSeeder: complete — {} users, {} branches, {} products, {} products seeded.",
            users.size(), branches.size(), categories.size(), products.size());
    }

    // ── Users ─────────────────────────────────────────────────────────

    private List<User> seedUsers() {
        Role superadmin    = roleRepository.findByName(RoleName.SUPERADMIN).orElseThrow();
        Role manager    = roleRepository.findByName(RoleName.MANAGER).orElseThrow();
        Role staff = roleRepository.findByName(RoleName.STAFF).orElseThrow();

        String pw = passwordEncoder.encode("Demo@12345");

        return userRepository.saveAll(List.of(
            user("Sophea Keo",    "owner@freshmart.kh",    pw, "+85512345678", superadmin),
            user("Dara Pich",     "dara@freshmart.kh",     pw, "+85512345679", manager),
            user("Sreyla Heng",   "sreyla@freshmart.kh",   pw, "+85512345680", manager),
            user("Mony Ros",      "mony@gmail.com",        pw, "+85512345681", staff),
            user("Bopha Chan",    "bopha@gmail.com",        pw, "+85512345682", staff),
            user("Rathana Sok",   "rathana@gmail.com",     pw, "+85512345683", staff),
            user("Visal Lim",     "visal@gmail.com",        pw, "+85512345684", staff),
            user("Channary Noun", "channary@gmail.com",    pw, "+85512345685", staff)
        ));
    }

    // ── Branches ─────────────────────────────────────────────────────

    private List<Branch> seedBranches() {
        return branchRepository.saveAll(List.of(
            branch("FreshMart — BKK1",
                "No. 15, Street 278, Boeng Keng Kang I, Phnom Penh",
                new BigDecimal("11.56372000"), new BigDecimal("104.92180000"), "+85523456001"),
            branch("FreshMart — Toul Tom Pong",
                "No. 48, Street 163, Toul Tom Pong I, Phnom Penh",
                new BigDecimal("11.54280000"), new BigDecimal("104.91730000"), "+85523456002"),
            branch("FreshMart — Sen Sok",
                "No. 200, National Road 6A, Sen Sok, Phnom Penh",
                new BigDecimal("11.59840000"), new BigDecimal("104.89550000"), "+85523456003")
        ));
    }

    private void assignManagerBranches(List<User> users, List<Branch> branches) {
        if (users.size() < 3 || branches.size() < 2) return;
        users.get(1).setManagedBranch(branches.get(0));
        users.get(2).setManagedBranch(branches.get(1));
        userRepository.saveAll(List.of(users.get(1), users.get(2)));
    }

    // ── Categories ────────────────────────────────────────────────────

    private List<Category> seedCategories() {
        return categoryRepository.saveAll(List.of(
            cat("Dairy & Refrigerated",  "Milk, eggs, cheese, yogurt"),
            cat("Beverages",             "Water, juice, soft drinks, tea, coffee"),
            cat("Snacks & Confectionery","Chips, crackers, chocolate, candy"),
            cat("Grains & Staples",      "Rice, noodles, oil, flour, sugar"),
            cat("Meat & Seafood",        "Fresh and frozen chicken, pork, beef, fish"),
            cat("Fruits & Vegetables",   "Fresh produce, seasonal fruits, greens"),
            cat("Condiments & Sauces",   "Soy sauce, fish sauce, chili sauce"),
            cat("Personal Care",         "Shampoo, soap, toothpaste, lotion"),
            cat("Household Cleaning",    "Dish liquid, laundry detergent, bleach"),
            cat("Baby & Child",          "Baby formula, diapers, baby food, wipes")
        ));
    }

    // ── Products ─────────────────────────────────────────────────────

    private List<Product> seedProducts(List<Category> cats) {
        Category dairy   = cats.get(0);
        Category bevs    = cats.get(1);
        Category snacks  = cats.get(2);
        Category grains  = cats.get(3);
        Category meat    = cats.get(4);
        Category produce = cats.get(5);
        Category sauces  = cats.get(6);
        Category care    = cats.get(7);
        Category clean   = cats.get(8);
        Category baby    = cats.get(9);

        return productRepository.saveAll(List.of(
            // Dairy
            prod("Fresh Milk 1L",           "Full cream pasteurised milk",                "8850001234567", dairy,   1.20,  2.50),
            prod("Greek Yogurt 200g",        "Plain full-fat Greek yogurt",                "8850001234568", dairy,   0.80,  1.75),
            prod("Cheddar Cheese 250g",      "Mild cheddar cheese block, imported",        "8850001234569", dairy,   2.50,  4.90),
            prod("Chicken Eggs 10pcs",       "Farm fresh eggs, grade A",                   "8850001234570", dairy,   1.80,  3.20),
            // Beverages
            prod("Mineral Water 1.5L",       "Natural mineral water, low sodium",          "8850002234567", bevs,    0.25,  0.75),
            prod("Orange Juice 1L",          "100% pure squeezed orange juice",            "8850002234568", bevs,    1.00,  2.20),
            prod("Green Tea 500ml",          "Japanese unsweetened green tea",             "8850002234569", bevs,    0.40,  1.00),
            prod("Cola 330ml Can",           "Carbonated cola soft drink",                 "8850002234570", bevs,    0.30,  0.80),
            // Snacks
            prod("Potato Chips 80g",         "Original salted potato chips",               "8850003234567", snacks,  0.50,  1.25),
            prod("Dark Chocolate 100g",      "70% cocoa dark chocolate, Belgian",          "8850003234568", snacks,  1.20,  2.80),
            prod("Cashew Nuts 150g",         "Roasted lightly salted cashews",             "8850003234569", snacks,  2.00,  4.50),
            // Grains
            prod("Jasmine Rice 5kg",         "Premium Thai jasmine rice",                  "8850004234567", grains,  4.50,  7.50),
            prod("Cooking Oil 1L",           "Pure sunflower cooking oil",                 "8850004234568", grains,  1.50,  2.80),
            prod("Rice Noodles 400g",        "Dried flat rice noodles",                    "8850004234569", grains,  0.80,  1.60),
            // Meat
            prod("Chicken Breast 500g",      "Boneless skinless chicken breast",           "8850005234567", meat,    3.00,  5.50),
            prod("Pork Mince 500g",          "Fresh ground pork, medium fat",              "8850005234568", meat,    2.80,  5.00),
            prod("Salmon Fillet 300g",       "Atlantic salmon fillet, skin-on",            "8850005234569", meat,    6.00, 10.50),
            // Produce
            prod("Banana (bunch ~6pcs)",     "Ripe Cambodian bananas",                     "8850006234567", produce, 0.60,  1.20),
            prod("Broccoli 400g",            "Fresh broccoli crown, pesticide-free",       "8850006234568", produce, 0.80,  1.80),
            prod("Cherry Tomatoes 250g",     "Sweet red cherry tomatoes, punnet",          "8850006234569", produce, 0.70,  1.50),
            // Sauces
            prod("Oyster Sauce 300ml",       "Premium oyster sauce, rich umami",           "8850007234567", sauces,  0.90,  2.00),
            prod("Fish Sauce 700ml",         "Traditional Cambodian fish sauce",           "8850007234568", sauces,  0.70,  1.60),
            prod("Sweet Chili Sauce 250ml",  "Thai sweet chili dipping sauce",             "8850007234569", sauces,  0.60,  1.40),
            // Personal care
            prod("Shampoo 400ml",            "Anti-dandruff shampoo, all hair types",      "8850008234567", care,    1.50,  3.20),
            prod("Toothpaste 150g",          "Whitening fluoride toothpaste",              "8850008234568", care,    0.80,  1.80),
            // Cleaning
            prod("Dishwashing Liquid 500ml", "Concentrated lemon dish liquid",             "8850009234567", clean,   0.70,  1.60),
            prod("Laundry Detergent 1kg",    "Powder detergent for white and colour",      "8850009234568", clean,   1.80,  3.50),
            // Baby
            prod("Baby Formula Stage 1 400g","Infant formula 0-6 months, DHA enriched",   "8850010234567", baby,    8.00, 14.50),
            prod("Diapers Size M 30pcs",     "Soft breathable diapers, 6-11 kg",           "8850010234568", baby,    5.50,  9.80),
            prod("Baby Wipes 80pcs",         "Fragrance-free gentle baby wipes",           "8850010234569", baby,    1.20,  2.60)
        ));
    }

    // ── Inventories ───────────────────────────────────────────────────

    private void seedInventories(List<Branch> branches, List<Product> products) {
        Branch b1 = branches.get(0);
        Branch b2 = branches.get(1);
        Branch b3 = branches.get(2);

        LocalDate today = LocalDate.now();

        // qty, threshold, daysToExpiry
        int[][] stockData = {
            // B1    B2    B3    thresh  daysExp
            { 45,   60,   35,   10,   7   },  // milk         – expiring soon
            { 30,   20,    9,   10,  14   },  // yogurt       – B3 low stock
            {  8,   15,   20,   10,  30   },  // cheese       – B1 low stock
            { 60,   80,   50,   15,  10   },  // eggs
            {200,  150,  180,   20, 365   },  // water
            { 40,   30,   35,   10,  21   },  // oj
            { 55,   40,   45,   10, 180   },  // green tea
            {120,   90,  110,   20, 270   },  // cola
            { 35,   50,   28,   10,  90   },  // chips
            { 25,   20,   32,   10, 120   },  // chocolate
            {  7,   18,   22,   10,  60   },  // cashew       – B1 low stock
            { 80,  100,   90,   20, 365   },  // rice
            { 45,   55,   40,   15, 365   },  // oil
            { 60,   70,   55,   15, 365   },  // noodles
            { 20,   25,   18,   10,   3   },  // chicken      – all expiring urgent
            { 15,   20,   12,   10,   3   },  // pork         – all expiring urgent
            {  6,   10,    8,   10,   2   },  // salmon       – low + expiring
            { 40,   55,   48,   10,   5   },  // banana
            { 22,   30,   25,   10,   7   },  // broccoli
            { 18,   25,   20,   10,   8   },  // tomatoes
            { 50,   60,   40,   10, 365   },  // oyster sauce
            { 45,   55,   50,   10, 365   },  // fish sauce
            { 38,   45,   35,   10, 365   },  // chili sauce
            { 30,   22,   18,   10, 730   },  // shampoo
            { 28,   35,   24,   10, 730   },  // toothpaste
            { 42,   50,   38,   10, 730   },  // dish liquid
            { 25,   30,   20,   10, 730   },  // laundry
            {  5,   12,   10,   10, 365   },  // formula      – B1 low stock
            { 18,   22,   16,   10, 730   },  // diapers
            { 24,   30,   28,   10, 730   },  // wipes
        };

        for (int i = 0; i < products.size(); i++) {
            Product p      = products.get(i);
            int[]   row    = stockData[i];
            int     thresh = row[3];
            LocalDate exp  = today.plusDays(row[4]);

            inventoryRepository.save(inv(b1, p, row[0], thresh, exp));
            inventoryRepository.save(inv(b2, p, row[1], thresh, exp));
            inventoryRepository.save(inv(b3, p, row[2], thresh, exp));
        }
    }


    // ── Builder helpers ───────────────────────────────────────────────

    private User user(String name, String email, String pw, String phone, Role role) {
        return User.builder().fullName(name).email(email).password(pw).phone(phone).role(role).build();
    }

    private Branch branch(String name, String address, BigDecimal lat, BigDecimal lng, String phone) {
        return Branch.builder().name(name).address(address).latitude(lat).longitude(lng).phone(phone).build();
    }

    private Category cat(String name, String desc) {
        return Category.builder().name(name).description(desc).build();
    }

    private Product prod(String name, String desc, String barcode,
                         Category cat, double cost, double sell) {
        return Product.builder()
            .name(name).description(desc).barcode(barcode).category(cat)
            .costPrice(BigDecimal.valueOf(cost))
            .sellingPrice(BigDecimal.valueOf(sell))
            .isActive(true).build();
    }

    private Inventory inv(Branch branch, Product product, int qty, int threshold, LocalDate expiry) {
        return Inventory.builder()
            .branch(branch).product(product)
            .quantity(qty).lowStockThreshold(threshold).expiryDate(expiry).build();
    }

    private record ItemSpec(Product product, int quantity) {}

    private ItemSpec item(Product p, int qty) { return new ItemSpec(p, qty); }

}
