package com.cuutrominhbach.repository;

import com.cuutrominhbach.entity.Role;
import com.cuutrominhbach.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findByRole(Role role);
    List<User> findByRoleAndProvince(Role role, String province);
    List<User> findByRoleAndIsApproved(Role role, Boolean isApproved);
    long countByRole(Role role);
    long countByRoleAndIsApproved(Role role, Boolean isApproved);
}
