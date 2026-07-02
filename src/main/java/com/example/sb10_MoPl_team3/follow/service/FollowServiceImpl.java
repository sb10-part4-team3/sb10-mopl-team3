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

    @Override
    public void cancel(UUID followerId, UUID followId) {
        Follow follow = followRepository.findById(followId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FOLLOW_NOT_FOUND));

        if (!follow.getFollower().getId().equals(followerId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        followRepository.delete(follow);
    }

    private FollowCreateResult createOrFindFollow(UUID followerId, UUID followeeId) {
        try {
            return new FollowCreateResult(createNewFollow(followerId, followeeId), true);
        } catch (DataIntegrityViolationException exception) {
            // 최초 조회 이후 다른 동시 요청이 같은 팔로우를 먼저 저장했을 수 있다.
            // 이 unique-key 경쟁은 기존 row를 반환해 멱등 성공으로 처리한다.
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
        // 중복 키 rollback이 바깥 흐름을 오염시키지 않도록 insert 시도를 별도 트랜잭션으로 격리한다.
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
