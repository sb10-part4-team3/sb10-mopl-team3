package com.example.sb10_MoPl_team3.follow.service;

import com.example.sb10_MoPl_team3.follow.dto.FollowDto;
import com.example.sb10_MoPl_team3.follow.dto.FollowRequest;
import com.example.sb10_MoPl_team3.follow.entity.Follow;
import com.example.sb10_MoPl_team3.follow.mapper.FollowMapper;
import com.example.sb10_MoPl_team3.follow.repository.FollowRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.exception.UserNotFoundException;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final FollowMapper followMapper;

    @Override
    public FollowDto create(UUID followerId, FollowRequest request) {
        UUID followeeId = request.followeeId();

        if (followerId.equals(followeeId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return followRepository.findByFollower_IdAndFollowee_Id(followerId, followeeId)
                .map(followMapper::toDto)
                .orElseGet(() -> createNewFollow(followerId, followeeId));
    }

    @Override
    public void cancel(UUID followerId, UUID followId) {
        Follow follow = followRepository.findById(followId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        if (!follow.getFollower().getId().equals(followerId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        followRepository.delete(follow);
    }

    private FollowDto createNewFollow(UUID followerId, UUID followeeId) {
        User follower = getUserOrThrow(followerId);
        User followee = getUserOrThrow(followeeId);

        Follow follow = Follow.builder()
                .follower(follower)
                .followee(followee)
                .build();

        return followMapper.toDto(followRepository.save(follow));
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
