package com.example.genz.repository.RewardRepository;

import com.example.genz.entity.Auth.User;
import com.example.genz.entity.Reward.XpTransaction;
import com.example.genz.enums.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface XpTransactionRepository extends JpaRepository<XpTransaction, UUID> {
    List<XpTransaction> findByUserOrderByCreatedAtDesc(User user);
    boolean existsByUserAndActivityTypeAndReferenceId(User user, ActivityType activityType, UUID referenceId);
}
