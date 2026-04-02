package com.reaksa.e_wingshop_api.service;

import com.reaksa.e_wingshop_api.entity.Role;
import com.reaksa.e_wingshop_api.entity.User;
import com.reaksa.e_wingshop_api.entity.Branch;
import com.reaksa.e_wingshop_api.enums.RoleName;
import com.reaksa.e_wingshop_api.exception.DuplicateResourceException;
import com.reaksa.e_wingshop_api.exception.ResourceNotFoundException;
import com.reaksa.e_wingshop_api.repository.BranchRepository;
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
    private final BranchRepository branchRepository;
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
                             String rawPassword, String phone, RoleName roleName, Long branchId) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already registered: " + email);
        }
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        Branch branch = null;
        if (roleName == RoleName.MANAGER && branchId != null) {
            branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
        }

        return userRepository.save(User.builder()
                .fullName(fullName)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .phone(phone)
                .role(role)
                .managedBranch(branch)
                .build());
    }

    /** Change a user's role — OWNER only. */
    @Transactional
    public User changeRole(Long userId, RoleName newRole) {
        User user = findById(userId);
        Role role = roleRepository.findByName(newRole)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + newRole));
        user.setRole(role);
        if (newRole != RoleName.MANAGER) {
            user.setManagedBranch(null);
        }
        return userRepository.save(user);
    }

    /** Assign a user as MANAGER of one branch. */
    @Transactional
    public User assignManagerToBranch(Long userId, Long branchId) {
        User user = findById(userId);
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
        Role managerRole = roleRepository.findByName(RoleName.MANAGER)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + RoleName.MANAGER));

        user.setRole(managerRole);
        user.setManagedBranch(branch);
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
