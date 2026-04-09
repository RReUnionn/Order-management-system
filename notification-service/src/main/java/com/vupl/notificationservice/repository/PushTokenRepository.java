package com.vupl.notificationservice.repository;

import com.vupl.notificationservice.entity.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushTokenRepository extends JpaRepository<PushToken, String> {
    List<PushToken> findByUserIdAndIsActiveTrue(String userId);

    Optional<PushToken> findByDeviceToken(String deviceToken);
}
