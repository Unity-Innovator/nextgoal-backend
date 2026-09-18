package com.example.genz.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.genz.auth.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    @org.springframework.data.jpa.repository.Query("""
            select u from User u
            where u.id <> :currentUserId and upper(u.status) = 'ACTIVE'
              and (lower(u.fullName) like lower(:namePattern) escape '!'
                   or u.phoneNumber = :phoneNumber)
              and not exists (select b.id from UserBlock b
                  where (b.blocker.id = :currentUserId and b.blockedUser.id = u.id)
                     or (b.blocker.id = u.id and b.blockedUser.id = :currentUserId))
            order by lower(u.fullName), u.id
            """)
    org.springframework.data.domain.Page<User> searchFriendCandidates(
            @org.springframework.data.repository.query.Param("currentUserId") UUID currentUserId,
            @org.springframework.data.repository.query.Param("namePattern") String namePattern,
            @org.springframework.data.repository.query.Param("phoneNumber") String phoneNumber,
            org.springframework.data.domain.Pageable pageable);

    Optional<User> findByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumber(String phoneNumber);
}
