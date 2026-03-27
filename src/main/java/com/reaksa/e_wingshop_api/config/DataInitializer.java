package com.reaksa.e_wingshop_api.config;

import com.reaksa.e_wingshop_api.entity.Role;
import com.reaksa.e_wingshop_api.entity.User;
import com.reaksa.e_wingshop_api.enums.RoleName;
import com.reaksa.e_wingshop_api.repository.RoleRepository;
import com.reaksa.e_wingshop_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder  passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedRoles();
        seedDefaultOwner();
    }

    // ── Roles ──────────────────────────────────────────────────────────
    private void seedRoles() {
        Arrays.stream(RoleName.values()).forEach(name -> {
            if (roleRepository.findByName(name).isEmpty()) {
                roleRepository.save(Role.builder().name(name).build());
                log.info("Seeded role: {}", name);
            }
        });
    }

    // ── Default owner account ─────────────────────────────────────────
    private void seedDefaultOwner() {
        String ownerEmail = "owner@grocery.local";
        if (userRepository.existsByEmail(ownerEmail)) return;

        Role ownerRole = roleRepository.findByName(RoleName.SUPERADMIN)
                .orElseThrow(() -> new IllegalStateException("OWNER role not found"));

        User owner = User.builder()
                .fullName("System Owner")
                .email(ownerEmail)
                .password(passwordEncoder.encode("Owner@12345"))
                .phone("+85500000000")
                .role(ownerRole)
                .build();

        userRepository.save(owner);
        log.warn("=======================================================");
        log.warn("  Default OWNER account created.");
        log.warn("  Email   : {}", ownerEmail);
        log.warn("  Password: Owner@12345");
        log.warn("  CHANGE THIS PASSWORD IMMEDIATELY IN PRODUCTION.");
        log.warn("=======================================================");
    }
}
