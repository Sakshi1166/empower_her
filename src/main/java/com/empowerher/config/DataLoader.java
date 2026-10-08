package com.empowerher.config;

import com.empowerher.entities.Category;
import com.empowerher.entities.Scheme;
import com.empowerher.entities.User;
import com.empowerher.repositories.CategoryRepository;
import com.empowerher.repositories.SchemeRepository;
import com.empowerher.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.create-default-users:false}")
    private boolean createDefaultUsers;

    @Value("${app.bootstrap.admin-username:}")
    private String adminUsername;

    @Value("${app.bootstrap.admin-password:}")
    private String adminPassword;

    @Value("${app.bootstrap.user-username:}")
    private String userUsername;

    @Value("${app.bootstrap.user-password:}")
    private String userPassword;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== INITIALIZING EMPOWERHER DATABASE ===");
        
        try {
            // Only create data if database is empty
            if (userRepository.count() == 0 && createDefaultUsers) {
                createDefaultUsers();
            } else {
                System.out.println(createDefaultUsers
                        ? "✓ Users already exist in database"
                        : "✓ Default user bootstrap is disabled");
            }
            
            if (categoryRepository.count() == 0) {
                createDefaultCategories();
            } else {
                System.out.println("✓ Categories already exist in database");
            }
            
            if (schemeRepository.count() == 0) {
                createSampleSchemes();
            } else {
                System.out.println("✓ Schemes already exist in database");
            }
            
            System.out.println("=== DATABASE INITIALIZATION COMPLETE ===");
        } catch (Exception e) {
            System.err.println("ERROR during database initialization: " + e.getMessage());
            System.out.println("Continuing application startup without initial data...");
        }
    }

    private void createDefaultUsers() {
        try {
            if (adminUsername.isBlank() || adminPassword.isBlank()
                    || userUsername.isBlank() || userPassword.isBlank()) {
                throw new IllegalStateException(
                        "Default user bootstrap requires all BOOTSTRAP_* username and password values");
            }

            // Create admin user
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setEmail("admin@empowerher.com");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole("ROLE_ADMIN");
            admin.setEnabled(true);
            admin.setCreatedAt(LocalDateTime.now());
            userRepository.save(admin);

            // Create sample user
            User user = new User();
            user.setUsername(userUsername);
            user.setEmail("user@empowerher.com");
            user.setPassword(passwordEncoder.encode(userPassword));
            user.setRole("ROLE_USER");
            user.setEnabled(true);
            user.setCreatedAt(LocalDateTime.now());
            userRepository.save(user);

            System.out.println("✓ Default users created in database");
        } catch (Exception e) {
            System.err.println("ERROR creating users: " + e.getMessage());
            throw e;
        }
    }

    private void createDefaultCategories() {
        try {
            Category central = new Category();
            central.setName("Central Government Schemes");
            central.setLevel("CENTRAL");
            categoryRepository.save(central);

            Category state = new Category();
            state.setName("State Government Schemes");
            state.setLevel("STATE");
            categoryRepository.save(state);

            Category district = new Category();
            district.setName("District Level Schemes");
            district.setLevel("DISTRICT");
            categoryRepository.save(district);

            Category ngo = new Category();
            ngo.setName("Private/NGO Schemes");
            ngo.setLevel("PRIVATE");
            categoryRepository.save(ngo);

            System.out.println("✓ Default categories created in database");
        } catch (Exception e) {
            System.err.println("ERROR creating categories: " + e.getMessage());
            throw e;
        }
    }

    private void createSampleSchemes() {
        try {
            Category centralCategory = categoryRepository.findByLevel("CENTRAL").get(0);
            
            // Sample scheme 1
            Scheme scheme1 = new Scheme();
            scheme1.setTitle("Beti Bachao Beti Padhao");
            scheme1.setCategory(centralCategory);
            scheme1.setLevel("CENTRAL");
            scheme1.setShortDesc("A scheme for girl child education and empowerment");
            scheme1.setFullDesc("The Beti Bachao Beti Padhao (BBBP) scheme was launched by the Government of India to address the declining Child Sex Ratio (CSR) and related issues of empowerment of women over a life-cycle continuum.");
            scheme1.setApplyLink("https://wcd.nic.in/bbbp-schemes");
            scheme1.setCreatedAt(LocalDateTime.now());
            scheme1.setActive(true);
            schemeRepository.save(scheme1);

            // Sample scheme 2
            Scheme scheme2 = new Scheme();
            scheme2.setTitle("Sukanya Samriddhi Yojana");
            scheme2.setCategory(centralCategory);
            scheme2.setLevel("CENTRAL");
            scheme2.setShortDesc("Small deposit scheme for girl child future education and marriage expenses");
            scheme2.setFullDesc("Sukanya Samriddhi Yojana is a government-backed savings scheme for girl children. The scheme encourages parents to build a fund for the future education and marriage expenses of their female child.");
            scheme2.setApplyLink("https://www.indiapost.gov.in/Financial/Pages/Content/Sukanya-Samriddhi-Account.aspx");
            scheme2.setCreatedAt(LocalDateTime.now());
            scheme2.setActive(true);
            schemeRepository.save(scheme2);

            System.out.println("✓ Sample schemes created in database");
        } catch (Exception e) {
            System.err.println("ERROR creating schemes: " + e.getMessage());
            throw e;
        }
    }
}