package com.example.chatvideo.repository;

import com.example.chatvideo.model.UserEntity;
import com.example.chatvideo.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    List<UserEntity> findByStatus(UserStatus status);
    boolean existsByEmail(String email);
}
