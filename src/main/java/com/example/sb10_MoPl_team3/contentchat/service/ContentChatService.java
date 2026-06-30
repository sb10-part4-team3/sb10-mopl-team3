package com.example.sb10_MoPl_team3.contentchat.service;

import com.example.sb10_MoPl_team3.contentchat.dto.ContentChatDto;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.mapper.UserMapper;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentChatService {

    private final UserRepository userRepository;

    public ContentChatDto createMessage(UUID contentId, UUID senderId, String content) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return new ContentChatDto(
                UUID.randomUUID(),
                contentId,
                Instant.now(),
                UserMapper.toSummary(sender),
                content
        );
    }
}
