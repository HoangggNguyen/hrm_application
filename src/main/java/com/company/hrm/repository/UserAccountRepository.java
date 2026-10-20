package com.company.hrm.repository;

import com.company.hrm.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "employee")
    List<UserAccount> findAllByOrderByUsername();
    Optional<UserAccount> findByUsername(String username);
    boolean existsByUsernameIgnoreCase(String username);
    Optional<UserAccount> findByEmployee_Id(Long employeeId);
    boolean existsByEmployee_Id(Long employeeId);
}