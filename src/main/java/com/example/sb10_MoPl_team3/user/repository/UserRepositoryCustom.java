package com.example.sb10_MoPl_team3.user.repository;

import com.example.sb10_MoPl_team3.user.dto.request.UserSearchCondition;
import com.example.sb10_MoPl_team3.user.entity.User;

import java.util.List;

public interface UserRepositoryCustom {

    List<User> searchUsers(UserSearchCondition condition, int limit);

    long countUsers(UserSearchCondition condition);
}