# Agentic Engineering System

A controlled agentic software-engineering platform demonstrated through URL
shortener engineering scenarios. The platform, rather than the URL shortener,
is the primary product.

## Current capabilities

- Explicit dependency graph with cycle and missing-dependency validation
- Sequential execution and parallel dependency-ready branches
- Synchronization before dependent tasks execute
- Versioned cross-stage workflow context
- Entry and exit gates
- Bounded task attempts
- Human release-approval gate
- Safe-stop workflow state
- Append-only in-memory decision lineage
- Pluggable task-handler boundary for later logical agents
- Approved repository-root enforcement and traversal protection
- Revision-isolated workspaces with immutable baseline snapshots
- SHA-256 manifests, cleanliness checks and verified rollback
- Controlled file listing, text reading and bounded code search
- Brownfield component, API, persistence and data-flow mapping
- Requirement-relevant bounded repository context for later model prompts

Model-backed reasoning, controlled patch/build tools, durable audit
storage, dynamic replanning and the three executable assessment scenarios are
delivered in later commits.

## Orchestration shape

```text
Requirement analysis
        |
        v
Repository analysis
        |
        v
Architecture
      /   \
     v     v
Implementation  Test generation
      \   /
       v v
Validation -> Documentation -> Human approval -> Completion
```

Tasks become runnable only when their dependencies and entry gate pass.
Independent ready tasks execute concurrently. Outputs are written to versioned
workflow context and must satisfy the task exit gate. Failures retry only within
the configured task bound; exhausted tasks fail the workflow.

## Technology

- Java 21
- Spring Boot 4.1.1
- Spring MVC
- PostgreSQL and Flyway
- Maven and JUnit
- Micrometer and Prometheus
- Docker Compose

## Verify

```powershell
docker compose config
docker compose up -d postgres
docker compose ps
.\mvnw.cmd clean test
```

Start the service:

```powershell
.\mvnw.cmd spring-boot:run
```

Check readiness:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health/readiness
```

## Delivery roadmap

1. Project bootstrap
2. Dependency-aware orchestration kernel
3. Isolated repository tools, snapshots and rollback
4. Deterministic and LLM-backed engineering model contracts
5. Specialized agents and ambiguity-driven replanning
6. Controlled patch generation and application
7. Executable validation and failure-driven repair
8. Governance, policy gates and durable audit evidence
9. Greenfield, brownfield and ambiguous scenario harness
10. CI, containers, metrics and consolidated engineering outcome
