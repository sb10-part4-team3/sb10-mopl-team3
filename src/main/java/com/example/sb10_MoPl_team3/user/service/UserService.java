package com.example.sb10_MoPl_team3.user.service;

import com.example.sb10_MoPl_team3.user.exception.DuplicatedEmailException;
import com.example.sb10_MoPl_team3.user.dto.request.UserCreateRequest;
import com.example.sb10_MoPl_team3.user.dto.response.UserDto;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.mapper.UserMapper;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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


        try {
            return UserMapper.toDto(userRepository.save(user));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicatedEmailException();
        }
    }
}
