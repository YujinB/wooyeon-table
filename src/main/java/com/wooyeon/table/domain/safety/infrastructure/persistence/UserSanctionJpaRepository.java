package com.wooyeon.table.domain.safety.infrastructure.persistence;

import com.wooyeon.table.domain.safety.domain.model.UserSanction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSanctionJpaRepository extends JpaRepository<UserSanction, Long> {

    List<UserSanction> findAllByUserIdOrderByIdDesc(Long userId);
}
