package com.formacionbdi.microservicios.app.usuarios.repository;

import com.formacionbdi.microservicios.app.usuarios.models.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.enabled = true")
    List<User> findAllEnabled();

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.roles r " +
            "ORDER BY u.id")
    List<User> findAllWithRoles();

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.roles r " +
            "WHERE u.id = :id")
    Optional<User> findByIdWithRoles(@Param("id") Long id);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    Optional<User> findByUsernameWithRoles(@Param("username") String username);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles WHERE u.enabled = true")
    List<User> findAllEnabledWithRoles();
}
