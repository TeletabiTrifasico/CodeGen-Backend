package com.banking.repository;

import com.banking.entity.Account;
import com.banking.entity.User;
import com.banking.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT a FROM Account a WHERE a.user.role = com.banking.enums.UserRole.CUSTOMER " +
           "AND a.user.approved = true AND a.active = true " +
           "AND (LOWER(a.user.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "OR LOWER(a.user.lastName) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "OR LOWER(CONCAT(a.user.firstName, ' ', a.user.lastName)) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<Account> searchByCustomerName(@Param("name") String name);
}
