package com.demo.trustdeskAi.repositories;

import com.demo.trustdeskAi.enitities.AITraceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AITraceRepository extends JpaRepository<AITraceEntity, Long> {
}
