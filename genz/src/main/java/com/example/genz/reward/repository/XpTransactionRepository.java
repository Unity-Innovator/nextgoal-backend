package com.example.genz.reward.repository;

import com.example.genz.auth.entity.User;
import com.example.genz.reward.entity.XpTransaction;
import com.example.genz.reward.enums.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface XpTransactionRepository extends JpaRepository<XpTransaction, UUID> {
    List<XpTransaction> findByUserOrderByCreatedAtDesc(User user);
    boolean existsByUserAndActivityTypeAndReferenceId(User user, ActivityType activityType, UUID referenceId);
}
