package com.santhosh.agentic_engineering_system.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEventJpaRepository extends JpaRepository<AuditEventEntity, Long> {
    List<AuditEventEntity> findByWorkflowIdOrderByIdAsc(UUID workflowId);
}
