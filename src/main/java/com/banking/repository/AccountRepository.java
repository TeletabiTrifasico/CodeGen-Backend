package com.banking.repository;

import com.banking.entity.Account;
import com.banking.entity.User;
import com.banking.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUserRole(UserRole role);
    Optional<Account> findByIban(String iban);
    List<Account> findByUser(User user);
    List<Account> findByUserUsername(String username);
    boolean existsByIban(String iban);
}
