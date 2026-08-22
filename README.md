# Agentic Engineering System

A controlled agentic software-engineering platform demonstrated through URL
shortener engineering scenarios. The platform, rather than the URL shortener,
is the primary product.

See [Architecture](docs/ARCHITECTURE.md) for the control loop, agent/model
boundary, trust boundaries, validation evidence and current limitations.
See the [Reviewer Guide](docs/REVIEWER-GUIDE.md) for reproducible greenfield,
brownfield and ambiguous end-to-end demonstrations.
See the [Engineering Outcome](docs/ENGINEERING-OUTCOME.md) for the consolidated
plan, rationale, artifacts, evidence, risks, assumptions and limitations.

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
- Provider-neutral structured engineering model contract
- Specialized requirement, architecture, implementation, testing, repair and
  documentation agents
- Deterministic offline provider for repeatable tests and demonstrations
- Real OpenAI Responses API provider with strict JSON-schema outputs
- Model request timeout, output bounds and environment-only credentials
- REST-submitted engineering workflows backed by isolated repository revisions
- Requirement-first execution with ambiguity-driven clarification pauses
- Dynamic scenario-aware task expansion after requirement interpretation
- Repository context flowing into architecture, implementation and test agents
- Generated implementation, test and documentation proposals retained in
  versioned workflow context
- Human approval required before release readiness can complete
- Preflight validation of generated create, update and delete operations
- Traversal, absolute-path, duplicate-path and unsupported-file rejection
- SHA-256 optimistic locking for updates and deletes
- Same-filesystem atomic writes with verified workspace rollback on failure
- Combined implementation/test patch application inside the isolated revision
- Changed-file and diff evidence returned by the workflow API
- Fixed-capability Maven execution inside the isolated repository
- Command timeout, output bounds and model-credential stripping
- Captured exit code, duration, timeout and full bounded build log
- Validation artifacts written for every attempt
- Compiler/test evidence passed to the repair agent
- Baseline restoration, corrected patch application and bounded revalidation
- Runnable Spring Boot URL-shortener fixture with baseline tests
- PostgreSQL-backed append-only workflow audit events
- Correlation IDs propagated through parallel task execution
- Explicit repository, patch, command, credential, retry and approval policies
- Actor-and-reason release approvals and safe-stop governance API
- Audit-event and policy inspection APIs
- Executable greenfield, brownfield analytics and ambiguous scenarios
- Scenario catalog API with expected evidence
- Prometheus workflow outcome/latency, task/retry, repair, rollback and recovery metrics

The agents now run as one stateful engineering workflow and safely apply their
structured source/test proposals inside an isolated revision. Generated changes
are compiled and tested; failures alter the next action by invoking repair and
revalidation. Durable audit storage, metrics and all three assessment scenarios
are included in this repository.

## Model providers

The default provider is deterministic and requires no credential:

```powershell
$env:MODEL_PROVIDER = "deterministic"
.\mvnw.cmd spring-boot:run
```

The same agent contracts can use the real OpenAI Responses API:

```powershell
$env:MODEL_PROVIDER = "openai"
$env:MODEL_API_KEY = Read-Host "OpenAI API key"
$env:MODEL_NAME = "gpt-4.1-mini"
.\mvnw.cmd spring-boot:run
```

API keys must remain in environment variables and must never be committed. The
OpenAI boundary is tested against a local mock HTTP server, so normal tests and
CI require neither network access nor paid credentials.

## Orchestration shape

```text
Requirement analysis
        |
        v
Repository analysis
        |
        v
Architecture
        |
        v
Implementation
      /       \
     v         v
Test generation  Documentation
     |
     v
Controlled patch -> Maven validation
                         |
             failure -> Repair agent -> rollback/reapply -> retry
                         |
                         v
             Human approval -> Completion
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
.\mvnw.cmd clean verify
```

Start the service:

```powershell
.\mvnw.cmd spring-boot:run
```

Check readiness:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health/readiness
```

## Submit an engineering workflow

The repository path must be relative to `AGENT_REPOSITORY_ROOT`, which defaults
to `./scenario-repositories`.

```powershell
$workflow = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/engineering-workflows" `
  -ContentType "application/json" `
  -Body (@{
    scenarioType = "BROWNFIELD"
    requirement = "Add total and daily redirect analytics"
    repositoryPath = "url-shortener"
  } | ConvertTo-Json)

$workflow | Select-Object id, status, contextRevision, validationAttempts, repaired
$workflow.tasks | Format-Table id, type, status, attempts, approvalRequired
$workflow.changedFiles
$workflow.diff
```

Every normal deterministic run reports `validationAttempts = 1` and
`repaired = false`. To exercise failure-driven adaptation, submit a requirement
containing the explicit phrase `repair scenario`. Deterministic mode first
generates a behavior that fails its generated test, then uses that real Maven
failure to repair and revalidate the patch:

```powershell
$repair = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/engineering-workflows" `
  -ContentType "application/json" `
  -Body (@{
    scenarioType = "BROWNFIELD"
    requirement = "Run the repair scenario while adding a reviewable generated capability"
    repositoryPath = "url-shortener"
  } | ConvertTo-Json)

$repair | Select-Object status, validationAttempts, repaired
```

Expected: `AWAITING_APPROVAL`, `2`, and `True`. Evidence is stored under
`agent-workspaces/<workflow-id>/revision-1/logs` and `artifacts`.

An ambiguous workflow returns `AWAITING_CLARIFICATION`. Resume it with:

```powershell
$workflow = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/engineering-workflows/$($workflow.id)/clarification" `
  -ContentType "application/json" `
  -Body (@{
    clarification = "Track total redirects per short code and daily UTC counts; expose both through a read-only API."
  } | ConvertTo-Json)
```

Clear workflows stop at `AWAITING_APPROVAL`. Select the release task and approve
it explicitly:

```powershell
$releaseTask = $workflow.tasks |
  Where-Object { $_.approvalRequired -eq $true } |
  Select-Object -First 1

$workflow = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/engineering-workflows/$($workflow.id)/tasks/$($releaseTask.id)/approval" `
  -ContentType "application/json" `
  -Body '{"actor":"reviewer@example.com","reason":"Validated diff and evidence reviewed"}'
```

Inspect durable governance evidence:

```powershell
Invoke-RestMethod `
  "http://localhost:8080/api/v1/engineering-workflows/$($workflow.id)/governance/audit-events" |
  Format-Table sequence, type, taskId, correlationId, occurredAt

Invoke-RestMethod `
  "http://localhost:8080/api/v1/engineering-workflows/$($workflow.id)/governance/policies" |
  Format-Table id, control, enforcement
```

Safely stop a non-terminal workflow:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/engineering-workflows/$($workflow.id)/governance/safe-stop" `
  -ContentType "application/json" `
  -Body '{"actor":"reviewer@example.com","reason":"Risk requires investigation"}'
```

Audit events survive application restarts. Active workflow execution state is
currently in memory and is intentionally documented as a limitation rather
than represented as restart-recoverable.

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

## Production delivery

`Dockerfile` creates a non-root Java 21 runtime image. `compose.yaml` starts the
application with PostgreSQL and health checks. GitHub Actions runs Maven verify,
enforces line coverage, uploads test/coverage evidence and verifies the image.

The JaCoCo HTML report is generated at `target/site/jacoco/index.html`.
