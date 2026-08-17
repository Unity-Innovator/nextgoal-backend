package com.example.genz.Repository.RewardRepository;

import com.example.genz.Entity.Auth.User;
import com.example.genz.Entity.Reward.XpTransaction;
import com.example.genz.Enums.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface XpTransactionRepository extends JpaRepository<XpTransaction, UUID> {
    List<XpTransaction> findByUserOrderByCreatedAtDesc(User user);
    boolean existsByUserAndActivityTypeAndReferenceId(User user, ActivityType activityType, UUID referenceId);
}
