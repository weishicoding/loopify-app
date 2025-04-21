package com.loopify.mainservice.service.user;


import com.loopify.mainservice.dto.user.UserDto;
import com.loopify.mainservice.model.user.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;

public interface UserService {

    UserDto getUserById(Long userId);

    UserDto getUserByEmail(Principal principal);

    void updateAvatar(Long userId, MultipartFile avatar) throws IOException;

    void updateBio(Long userId, String bio);

    void updateAddress(Long userId, String address);

    void updateNickname(Long userId, String nickname);
}
