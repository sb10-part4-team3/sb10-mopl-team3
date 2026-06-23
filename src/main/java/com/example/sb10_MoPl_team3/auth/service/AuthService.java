package com.example.sb10_MoPl_team3.auth.service;

import com.example.sb10_MoPl_team3.auth.dto.request.SignInRequest;
import com.example.sb10_MoPl_team3.auth.dto.response.JwtDto;
import com.example.sb10_MoPl_team3.auth.exception.InvalidCredentialException;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserStatus;
import com.example.sb10_MoPl_team3.user.mapper.UserMapper;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public JwtDto signIn(SignInRequest request) {
        User user = userRepository.findByEmail(request.username())
                .orElseThrow(InvalidCredentialException::new);

        if (user.getStatus() == UserStatus.LOCKED || user.getStatus() == UserStatus.WITHDRAWN)
            throw new InvalidCredentialException();

        if (!passwordEncoder.matches(request.password(), user.getPassword()))
            throw new InvalidCredentialException();

        String accessToken = tokenService.issueAccessToken(user);

        return new JwtDto(
                UserMapper.toDto(user),
                accessToken
        );
    }
}
