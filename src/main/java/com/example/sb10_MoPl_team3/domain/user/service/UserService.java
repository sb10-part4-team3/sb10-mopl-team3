package com.example.sb10_MoPl_team3.domain.user.service;

import com.example.sb10_MoPl_team3.domain.user.exception.DuplicatedEmailException;
import com.example.sb10_MoPl_team3.domain.user.dto.request.UserCreateRequest;
import com.example.sb10_MoPl_team3.domain.user.dto.response.UserDto;
import com.example.sb10_MoPl_team3.domain.user.entity.User;
import com.example.sb10_MoPl_team3.domain.user.enums.UserRole;
import com.example.sb10_MoPl_team3.domain.user.mapper.UserMapper;
import com.example.sb10_MoPl_team3.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDto createUser(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicatedEmailException();
        }

        User user = new User(
                request.email(),
                request.name(),
                passwordEncoder.encode(request.password()),
                null,
                UserRole.USER
        );

        return UserMapper.toDto(userRepository.save(user));
    }
}
