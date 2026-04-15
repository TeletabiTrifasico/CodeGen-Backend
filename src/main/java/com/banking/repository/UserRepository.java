package com.banking.repository;

import com.banking.entity.User;
import com.banking.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByBsn(String bsn);
    List<User> findByApprovedAndRole(boolean approved, UserRole role);
    List<User> findByRole(UserRole role);
}
