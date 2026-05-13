package com.rpa.manage.domain.repository;

import com.rpa.manage.domain.entity.AiModelConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiModelConfigRepository extends JpaRepository<AiModelConfig, Long> {

    Optional<AiModelConfig> findByConfigKey(String configKey);
}
