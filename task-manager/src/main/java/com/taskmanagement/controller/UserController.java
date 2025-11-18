package com.taskmanagement.controller;

import com.taskmanagement.dto.UserDTO;
import com.taskmanagement.dto.CreateUserRequest;
import com.taskmanagement.dto.UpdateUserRequest;
import com.taskmanagement.entity.User;
import com.taskmanagement.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    private final UserRepo userRepository;
    private final PasswordEncoder passwordEncoder;

    // Get all users
    @GetMapping
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Get user by ID
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Integer id) {
        return userRepository.findById(id)
                .map(this::convertToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Create new user
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody CreateUserRequest request) {
        // Check if username or email already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().build();
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().build();
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(User.UserRole.valueOf(request.getRole()));
        user.setStatus(User.UserStatus.ACTIVE);
        user.setAvatarColor(request.getAvatarColor() != null ? request.getAvatarColor() : generateRandomColor());

        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(convertToDTO(savedUser));
    }

    // Update user
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Integer id,
                                              @RequestBody UpdateUserRequest request) {
        return userRepository.findById(id)
                .map(user -> {
                    if (request.getFullName() != null) user.setFullName(request.getFullName());
                    if (request.getEmail() != null) user.setEmail(request.getEmail());
                    if (request.getRole() != null) user.setRole(User.UserRole.valueOf(request.getRole()));
                    if (request.getStatus() != null) user.setStatus(User.UserStatus.valueOf(request.getStatus()));
                    if (request.getAvatarColor() != null) user.setAvatarColor(request.getAvatarColor());

                    // Update password if provided
                    if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
                    }

                    User updatedUser = userRepository.save(user);
                    return ResponseEntity.ok(convertToDTO(updatedUser));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Delete user (actually just deactivate)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setStatus(User.UserStatus.INACTIVE);
                    userRepository.save(user);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Activate user
    @PutMapping("/{id}/activate")
    public ResponseEntity<UserDTO> activateUser(@PathVariable Integer id) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setStatus(User.UserStatus.ACTIVE);
                    User activatedUser = userRepository.save(user);
                    return ResponseEntity.ok(convertToDTO(activatedUser));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private UserDTO convertToDTO(User user) {
        return UserDTO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().toString())
                .status(user.getStatus().toString())
                .avatarColor(user.getAvatarColor())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String generateRandomColor() {
        String[] colors = {"#5B8DEF", "#5ECFB1", "#F5A864", "#F56565", "#9F7AEA", "#48BB78"};
        return colors[(int) (Math.random() * colors.length)];
    }
}