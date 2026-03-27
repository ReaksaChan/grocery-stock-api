package com.reaksa.e_wingshop_api.service;

import com.reaksa.e_wingshop_api.entity.Role;
import com.reaksa.e_wingshop_api.entity.User;
import com.reaksa.e_wingshop_api.enums.RoleName;
import com.reaksa.e_wingshop_api.exception.DuplicateResourceException;
import com.reaksa.e_wingshop_api.exception.ResourceNotFoundException;
import com.reaksa.e_wingshop_api.repository.RoleRepository;
import com.reaksa.e_wingshop_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<User> findAll(int page, int size) {
        return userRepository.findAll(
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmailWithRole(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    /** OWNER creates staff/admin accounts directly. */
    @Transactional
    public User createStaff(String fullName, String email,
                             String rawPassword, String phone, RoleName roleName) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already registered: " + email);
        }
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        return userRepository.save(User.builder()
                .fullName(fullName)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .phone(phone)
                .role(role)
                .build());
    }

    /** Change a user's role — OWNER only. */
    @Transactional
    public User changeRole(Long userId, RoleName newRole) {
        User user = findById(userId);
        Role role = roleRepository.findByName(newRole)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + newRole));
        user.setRole(role);
        return userRepository.save(user);
    }

    /** Reset a user's password — OWNER / ADMIN. */
    @Transactional
    public void resetPassword(Long userId, String newRawPassword) {
        User user = findById(userId);
        user.setPassword(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
    }

    /** Customer updates their own profile. */
    @Transactional
    public User updateProfile(Long userId, String fullName, String phone) {
        User user = findById(userId);
        if (fullName != null && !fullName.isBlank()) user.setFullName(fullName);
        if (phone    != null && !phone.isBlank())    user.setPhone(phone);
        return userRepository.save(user);
    }
}
