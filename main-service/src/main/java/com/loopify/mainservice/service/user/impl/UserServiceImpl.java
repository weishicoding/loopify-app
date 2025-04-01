package com.loopify.mainservice.service.user.impl;

import com.loopify.mainservice.exception.AppException;
import com.loopify.mainservice.model.user.User;
import com.loopify.mainservice.repository.user.UserRepository;
import com.loopify.mainservice.service.file.MinioService;
import com.loopify.mainservice.service.user.UserService;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final MinioService minioService;

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found"));
    }

    @Override
    @Transactional
    public void updateAvatar(Long userId, MultipartFile avatar) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found"));
        if (avatar != null) {
            // delete old avatar
            if (StringUtils.isNotEmpty(user.getAvatarUrl())) {
                minioService.deleteFile(user.getAvatarUrl());
            }
            String avatarUrl = minioService.uploadFile(avatar, "avatars", true);
            user.setAvatarUrl(avatarUrl);
        }
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateBio(Long userId, String bio) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found"));
        user.setBio(bio);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateAddress(Long userId, String address) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found"));
        user.setAddress(address);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateNickname(Long userId, String nickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found"));
        user.setNickname(nickname);
        userRepository.save(user);
    }
}
