package com.fsm.keystone.repository;

import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    @Query("""
            select u from AppUser u
            left join fetch u.customer
            where u.email = :email
            """)
    Optional<AppUser> findByEmailWithCustomer(@Param("email") String email);

    boolean existsByEmail(String email);

    long countByRoleAndActive(Role role, Boolean active);

    List<AppUser> findByRole(Role role);

    // Fetch all users except MANAGER
    List<AppUser> findByRoleNot(Role role);

    // Optional: Fetch only active users except MANAGER
    List<AppUser> findByRoleNotAndActiveTrue(Role role);

    List<AppUser> findByCustomer_Id(Long customerId);

    @Query("""
            select u from AppUser u
            left join fetch u.customer
            where u.id = :id
            """)
    Optional<AppUser> findByIdWithCustomer(@Param("id") Long id);

    @Query("""
            select u from AppUser u
            left join fetch u.customer
            where u.role <> :role
            """)
    List<AppUser> findByRoleNotWithCustomer(@Param("role") Role role);

    @Query("""
            select u from AppUser u
            left join fetch u.customer
            where u.role in :roles
            order by u.fullName asc
            """)
    List<AppUser> findByRoleInWithCustomer(@Param("roles") Collection<Role> roles);
}