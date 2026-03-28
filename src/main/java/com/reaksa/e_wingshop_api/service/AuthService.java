package com.reaksa.e_wingshop_api.service;

import com.reaksa.e_wingshop_api.dto.request.LoginRequest;
import com.reaksa.e_wingshop_api.dto.request.RegisterRequest;
import com.reaksa.e_wingshop_api.dto.response.AuthResponse;
import com.reaksa.e_wingshop_api.entity.Role;
import com.reaksa.e_wingshop_api.entity.User;
import com.reaksa.e_wingshop_api.enums.RoleName;
import com.reaksa.e_wingshop_api.exception.DuplicateResourceException;
import com.reaksa.e_wingshop_api.exception.ResourceNotFoundException;
import com.reaksa.e_wingshop_api.repository.RoleRepository;
import com.reaksa.e_wingshop_api.repository.UserRepository;
import com.reaksa.e_wingshop_api.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        Role customerRole = roleRepository.findByName(RoleName.VIEWER)
                .orElseThrow(() -> new ResourceNotFoundException("Role Viewer not found"));

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(customerRole)
                .build();

        userRepository.save(user);

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmailWithRole(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return buildAuthResponse(user);
    }

    public AuthResponse refreshToken(String refreshToken) {
        String email = jwtUtil.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (!jwtUtil.isTokenValid(refreshToken, userDetails)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        User user = userRepository.findByEmailWithRole(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        Map<String, Object> claims = Map.of(
                "role", user.getRole().getName().name(),
                "userId", user.getId()
        );

        String accessToken = jwtUtil.generateToken(userDetails, claims);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().getName().name())
                .build();
    }
}
