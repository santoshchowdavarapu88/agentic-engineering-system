package com.santhosh.agentic_engineering_system.orchestration.snapshot;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

interface WorkflowSnapshotJpaRepository extends JpaRepository<WorkflowSnapshotEntity, UUID> { }
