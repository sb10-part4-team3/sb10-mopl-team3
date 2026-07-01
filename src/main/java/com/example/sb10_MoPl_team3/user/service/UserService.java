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
import com.example.sb10_MoPl_team3.global.file.FileStorageService;
import com.example.sb10_MoPl_team3.global.security.UserAuthorizationService;
import com.example.sb10_MoPl_team3.user.dto.request.UserUpdateRequest;
import com.example.sb10_MoPl_team3.user.exception.UserNotFoundException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final UserAuthorizationService userAuthorizationService;

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

    public UserDto findUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return UserMapper.toDto(user);
    }

    @Transactional
    public UserDto updateUser(UUID userId, UserUpdateRequest request, MultipartFile image) {
        userAuthorizationService.validateSelf(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String previousProfileImageUrl = user.getProfileImageUrl();
        String uploadedProfileImageUrl = null;

        try {
            if (image != null && !image.isEmpty()) {
                uploadedProfileImageUrl = fileStorageService.upload(image);
                registerProfileImageCleanup(previousProfileImageUrl, uploadedProfileImageUrl);
            }

            user.updateProfile(request.name(), uploadedProfileImageUrl);
            userRepository.flush();

            return UserMapper.toDto(user);
        } catch (RuntimeException exception) {
            if (uploadedProfileImageUrl != null
                    && !TransactionSynchronizationManager.isSynchronizationActive()) {
                fileStorageService.deleteByUrl(uploadedProfileImageUrl);
            }

            throw exception;
        }
    }

    private void registerProfileImageCleanup(
            String previousProfileImageUrl,
            String uploadedProfileImageUrl
    ) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deletePreviousProfileImage(previousProfileImageUrl, uploadedProfileImageUrl);
            }

            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    fileStorageService.deleteByUrl(uploadedProfileImageUrl);
                }
            }
        });
    }

    private void deletePreviousProfileImage(
            String previousProfileImageUrl,
            String uploadedProfileImageUrl
    ) {
        if (previousProfileImageUrl == null || previousProfileImageUrl.isBlank()) {
            return;
        }

        if (previousProfileImageUrl.equals(uploadedProfileImageUrl)) {
            return;
        }

        fileStorageService.deleteByUrl(previousProfileImageUrl);
    }
}
