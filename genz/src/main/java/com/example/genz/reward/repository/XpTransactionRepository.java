package com.example.genz.reward.repository;

import com.example.genz.auth.entity.User;
import com.example.genz.reward.entity.XpTransaction;
import com.example.genz.reward.enums.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface XpTransactionRepository extends JpaRepository<XpTransaction, UUID> {
    List<XpTransaction> findByUserOrderByCreatedAtDesc(User user);
    boolean existsByUserAndActivityTypeAndReferenceId(User user, ActivityType activityType, UUID referenceId);

    @Query("SELECT COALESCE(SUM(xt.points), 0) FROM XpTransaction xt WHERE xt.user.id = :userId AND xt.createdAt >= :start AND xt.createdAt <= :end AND xt.activityType <> :excludedType")
    int sumPointsByUserIdAndCreatedAtBetweenExcludingType(
            @Param("userId") UUID userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("excludedType") ActivityType excludedType
    );
}
