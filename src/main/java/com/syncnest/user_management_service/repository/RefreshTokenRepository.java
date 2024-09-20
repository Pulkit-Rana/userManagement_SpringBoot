package com.syncnest.user_management_service.repository;

import com.syncnest.user_management_service.entity.RefreshToken;
import com.syncnest.user_management_service.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUserInfoOrderByExpiryDateDesc(UserInfo userInfo);

    List<RefreshToken> findByUserInfo(UserInfo userInfo);
}