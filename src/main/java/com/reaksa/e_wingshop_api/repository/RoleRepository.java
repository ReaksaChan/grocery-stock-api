package com.reaksa.e_wingshop_api.repository;

import com.reaksa.e_wingshop_api.entity.Role;
import com.reaksa.e_wingshop_api.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
