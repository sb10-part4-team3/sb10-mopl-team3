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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Transactional
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final FollowMapper followMapper;
    private final PlatformTransactionManager transactionManager;

    @Override
    public FollowCreateResult create(UUID followerId, FollowRequest request) {
        UUID followeeId = request.followeeId();

        if (followerId.equals(followeeId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return findFollow(followerId, followeeId)
                .map(follow -> new FollowCreateResult(follow, false))
                .orElseGet(() -> createOrFindFollow(followerId, followeeId));
    }



    private FollowCreateResult createOrFindFollow(UUID followerId, UUID followeeId) {
        try {
            return new FollowCreateResult(createNewFollow(followerId, followeeId), true);
        } catch (DataIntegrityViolationException exception) {
            return findFollow(followerId, followeeId)
                    .map(follow -> new FollowCreateResult(follow, false))
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.INVALID_INPUT_VALUE,
                            Map.of(
                                    "followerId", followerId,
                                    "followeeId", followeeId
                            ),
                            exception
                    ));
        }
    }

    private FollowDto createNewFollow(UUID followerId, UUID followeeId) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        return transactionTemplate.execute(status -> {
            User follower = getUserOrThrow(followerId);
            User followee = getUserOrThrow(followeeId);

            Follow follow = Follow.builder()
                    .follower(follower)
                    .followee(followee)
                    .build();

            return followMapper.toDto(followRepository.saveAndFlush(follow));
        });
    }

    private Optional<FollowDto> findFollow(UUID followerId, UUID followeeId) {
        return followRepository.findByFollower_IdAndFollowee_Id(followerId, followeeId)
                .map(followMapper::toDto);
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
