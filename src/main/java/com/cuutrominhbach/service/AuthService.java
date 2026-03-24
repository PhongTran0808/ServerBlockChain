package com.cuutrominhbach.service;

import com.cuutrominhbach.dto.request.LoginRequest;
import com.cuutrominhbach.dto.response.LoginResponse;
import com.cuutrominhbach.entity.User;
import com.cuutrominhbach.exception.AuthException;
import com.cuutrominhbach.repository.UserRepository;
import com.cuutrominhbach.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new AuthException("Tên đăng nhập hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(request.password(), user.getHashPassword())) {
            throw new AuthException("Tên đăng nhập hoặc mật khẩu không đúng");
        }

        String token = jwtTokenProvider.generateToken(user);

        return new LoginResponse(
                token,
                user.getRole().name(),
                user.getWalletAddress(),
                user.getId()
        );
    }
}
