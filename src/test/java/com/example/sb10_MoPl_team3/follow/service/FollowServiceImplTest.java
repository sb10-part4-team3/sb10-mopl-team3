package com.example.sb10_MoPl_team3.follow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.sb10_MoPl_team3.follow.dto.FollowDto;
import com.example.sb10_MoPl_team3.follow.dto.FollowRequest;
import com.example.sb10_MoPl_team3.follow.entity.Follow;
import com.example.sb10_MoPl_team3.follow.mapper.FollowMapper;
import com.example.sb10_MoPl_team3.follow.repository.FollowRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.exception.UserNotFoundException;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FollowServiceImplTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FollowMapper followMapper;

    @InjectMocks
    private FollowServiceImpl followService;

    @Test
    @DisplayName("create saves follow from authenticated follower to requested followee")
    void create_success() {
        UUID followerId = uuid(1);
        UUID followeeId = uuid(2);
        UUID followId = uuid(10);
        User follower = user(followerId, "follower@test.com", "follower");
        User followee = user(followeeId, "followee@test.com", "followee");
        Follow saved = follow(followId, follower, followee);
        FollowDto dto = new FollowDto(followId, followeeId, followerId);

        given(followRepository.findByFollower_IdAndFollowee_Id(followerId, followeeId))
                .willReturn(Optional.empty());
        given(userRepository.findById(followerId)).willReturn(Optional.of(follower));
        given(userRepository.findById(followeeId)).willReturn(Optional.of(followee));
        given(followRepository.saveAndFlush(any(Follow.class))).willReturn(saved);
        given(followMapper.toDto(saved)).willReturn(dto);

        FollowDto response = followService.create(followerId, new FollowRequest(followeeId));

        assertThat(response).isEqualTo(dto);
        ArgumentCaptor<Follow> followCaptor = ArgumentCaptor.forClass(Follow.class);
        then(followRepository).should().saveAndFlush(followCaptor.capture());
        assertThat(followCaptor.getValue().getFollower()).isEqualTo(follower);
        assertThat(followCaptor.getValue().getFollowee()).isEqualTo(followee);
    }

    @Test
    @DisplayName("create returns existing follow when the relationship already exists")
    void create_existingFollow() {
        UUID followerId = uuid(1);
        UUID followeeId = uuid(2);
        UUID followId = uuid(10);
        Follow existing = follow(
                followId,
                user(followerId, "follower@test.com", "follower"),
                user(followeeId, "followee@test.com", "followee")
        );
        FollowDto dto = new FollowDto(followId, followeeId, followerId);

        given(followRepository.findByFollower_IdAndFollowee_Id(followerId, followeeId))
                .willReturn(Optional.of(existing));
        given(followMapper.toDto(existing)).willReturn(dto);

        FollowDto response = followService.create(followerId, new FollowRequest(followeeId));

        assertThat(response).isEqualTo(dto);
        then(userRepository).should(never()).findById(any());
        then(followRepository).should(never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("create rejects self follow")
    void create_selfFollow() {
        UUID userId = uuid(1);

        assertThatThrownBy(() -> followService.create(userId, new FollowRequest(userId)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
                );

        then(followRepository).should(never()).findByFollower_IdAndFollowee_Id(any(), any());
        then(followRepository).should(never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("create converts duplicate key race into business exception")
    void create_duplicateRace() {
        UUID followerId = uuid(1);
        UUID followeeId = uuid(2);
        User follower = user(followerId, "follower@test.com", "follower");
        User followee = user(followeeId, "followee@test.com", "followee");

        given(followRepository.findByFollower_IdAndFollowee_Id(followerId, followeeId))
                .willReturn(Optional.empty());
        given(userRepository.findById(followerId)).willReturn(Optional.of(follower));
        given(userRepository.findById(followeeId)).willReturn(Optional.of(followee));
        given(followRepository.saveAndFlush(any(Follow.class)))
                .willThrow(new DataIntegrityViolationException("uk_follower_followee"));

        assertThatThrownBy(() -> followService.create(followerId, new FollowRequest(followeeId)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
                    assertThat(exception.getDetails())
                            .containsEntry("followerId", followerId)
                            .containsEntry("followeeId", followeeId);
                });

        then(followMapper).should(never()).toDto(any());
    }

    @Test
    @DisplayName("create rejects missing followee")
    void create_followeeNotFound() {
        UUID followerId = uuid(1);
        UUID followeeId = uuid(2);
        User follower = user(followerId, "follower@test.com", "follower");

        given(followRepository.findByFollower_IdAndFollowee_Id(followerId, followeeId))
                .willReturn(Optional.empty());
        given(userRepository.findById(followerId)).willReturn(Optional.of(follower));
        given(userRepository.findById(followeeId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> followService.create(followerId, new FollowRequest(followeeId)))
                .isInstanceOf(UserNotFoundException.class);

        then(followRepository).should(never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("cancel deletes own follow")
    void cancel_success() {
        UUID followerId = uuid(1);
        UUID followeeId = uuid(2);
        UUID followId = uuid(10);
        Follow follow = follow(
                followId,
                user(followerId, "follower@test.com", "follower"),
                user(followeeId, "followee@test.com", "followee")
        );

        given(followRepository.findById(followId)).willReturn(Optional.of(follow));

        followService.cancel(followerId, followId);

        then(followRepository).should().delete(follow);
    }

    @Test
    @DisplayName("cancel rejects another user's follow")
    void cancel_accessDenied() {
        UUID ownerId = uuid(1);
        UUID requestUserId = uuid(3);
        UUID followId = uuid(10);
        Follow follow = follow(
                followId,
                user(ownerId, "owner@test.com", "owner"),
                user(uuid(2), "followee@test.com", "followee")
        );

        given(followRepository.findById(followId)).willReturn(Optional.of(follow));

        assertThatThrownBy(() -> followService.cancel(requestUserId, followId))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED)
                );

        then(followRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("cancel rejects missing follow id")
    void cancel_notFound() {
        UUID followerId = uuid(1);
        UUID followId = uuid(10);

        given(followRepository.findById(followId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> followService.cancel(followerId, followId))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FOLLOW_NOT_FOUND)
                );

        then(followRepository).should(never()).delete(any());
    }

    private Follow follow(UUID id, User follower, User followee) {
        Follow follow = Follow.builder()
                .follower(follower)
                .followee(followee)
                .build();
        ReflectionTestUtils.setField(follow, "id", id);
        return follow;
    }

    private User user(UUID id, String email, String name) {
        User user = new User(email, name, "password", null, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private UUID uuid(int value) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", value));
    }
}
