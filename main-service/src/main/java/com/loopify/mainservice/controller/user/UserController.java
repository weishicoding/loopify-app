package com.loopify.mainservice.controller.user;

import com.loopify.mainservice.dto.user.UserDto;
import com.loopify.mainservice.exception.AppException;
import com.loopify.mainservice.model.user.User;
import com.loopify.mainservice.repository.user.UserRepository;
import com.loopify.mainservice.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(@PathVariable Long userId) {
        try {
            UserDto user = userService.getUserById(userId);
            return ResponseEntity.ok(user);
        } catch (AppException e) {
            log.error(e.getMessage());
            return ResponseEntity.internalServerError().body(e.getMessage());
        }

    }

    @GetMapping("/me")
    public ResponseEntity<?> getUser(Principal principal) {
        try {
            UserDto user = userService.getUserByEmail(principal);
            return ResponseEntity.ok(user);
        } catch (AppException e) {
            log.error(e.getMessage());
            return ResponseEntity.internalServerError().body(e.getMessage());
        }

    }

    @PutMapping("/updateAvatar/{userId}")
    @PreAuthorize("isAuthenticated")
    public ResponseEntity<?> updateAvatar(@PathVariable Long userId,
                                        @RequestParam MultipartFile avatar) {
        try {
            userService.updateAvatar(userId, avatar);
            return ResponseEntity.ok().build();
        }catch (IOException | AppException exception) {
            log.error(exception.getMessage(), exception);
            return ResponseEntity.internalServerError().body(exception.getMessage());
        }
    }

    @PutMapping("/updateBio/{userId}")
    @PreAuthorize("isAuthenticated")
    public ResponseEntity<?> updateBio(@PathVariable Long userId,
                                       @RequestParam(required = false) String bio) {
        try {
            userService.updateBio(userId, bio);
            return ResponseEntity.ok().build();
        }catch (AppException exception) {
            log.error(exception.getMessage(), exception);
            return ResponseEntity.internalServerError().body(exception.getMessage());
        }
    }

    @PutMapping("/updateNickname/{userId}")
    @PreAuthorize("isAuthenticated")
    public ResponseEntity<?> updateNickname(@PathVariable Long userId,
                                       @RequestParam String nickname) {
        try {
            userService.updateNickname(userId, nickname);
            return ResponseEntity.ok().build();
        }catch (AppException exception) {
            log.error(exception.getMessage(), exception);
            return ResponseEntity.internalServerError().body(exception.getMessage());
        }
    }

    @PutMapping("/updateAddress/{userId}")
    @PreAuthorize("isAuthenticated")
    public ResponseEntity<?> updateAddress(@PathVariable Long userId,
                                       @RequestParam(required = false) String address) {
        try {
            userService.updateAddress(userId, address);
            return ResponseEntity.ok().build();
        }catch (AppException exception) {
            log.error(exception.getMessage(), exception);
            return ResponseEntity.internalServerError().body(exception.getMessage());
        }
    }
}
