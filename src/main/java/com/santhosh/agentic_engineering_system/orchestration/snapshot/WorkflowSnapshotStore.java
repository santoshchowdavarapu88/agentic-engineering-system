package com.santhosh.agentic_engineering_system.orchestration.snapshot;

import com.santhosh.agentic_engineering_system.orchestration.domain.EngineeringWorkflow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowSnapshotStore {
    private final WorkflowSnapshotJpaRepository repository;
    private final Clock clock;

    @Transactional
    public void checkpoint(EngineeringWorkflow workflow) {
        String keys = workflow.getContext().snapshot().keySet().stream()
                .sorted().collect(Collectors.joining(","));
        String tasks = workflow.getTasks().stream()
                .sorted(Comparator.comparing(task -> task.getId().toString()))
                .map(task -> task.getId() + ":" + task.getType() + ":" + task.getStatus()
                        + ":attempts=" + task.getAttempts())
                .collect(Collectors.joining("\n"));
        repository.save(new WorkflowSnapshotEntity(workflow.getId(), workflow.getRequirement(),
                workflow.getStatus(), workflow.getFailureMessage(), workflow.getContext().revision(),
                keys, tasks, Instant.now(clock)));
    }

    @Transactional(readOnly = true)
    public WorkflowSnapshotView require(UUID id) {
        var entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workflow snapshot does not exist"));
        return new WorkflowSnapshotView(entity.getWorkflowId(), entity.getRequirement(),
                entity.getStatus(), entity.getFailureMessage(), entity.getContextRevision(),
                entity.getContextKeys(), entity.getTaskStates(), entity.getUpdatedAt());
    }
}
