package com.cuutrominhbach.security;

import com.cuutrominhbach.entity.User;
import com.cuutrominhbach.repository.UserRepository;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Aspect
@Component
public class WalletGuardAspect {

    private final UserRepository userRepository;

    public WalletGuardAspect(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Before("@annotation(com.cuutrominhbach.security.RequireWallet)")
    public void checkWallet(JoinPoint joinPoint) {
        Long userId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("Người dùng không tồn tại"));

        if (user.getWalletAddress() == null || user.getWalletAddress().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bắt buộc phải cập nhật địa chỉ ví Web3 trước khi thực hiện giao dịch này."
            );
        }
    }
}
