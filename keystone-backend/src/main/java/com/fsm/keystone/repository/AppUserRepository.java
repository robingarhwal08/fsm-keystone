package com.fsm.keystone.repository;


import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRoleAndActive(Role role, Boolean active);

    List<AppUser> findByRole(Role role);
}
