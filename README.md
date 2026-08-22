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

The agents now run as one stateful engineering workflow and safely apply their
structured source/test proposals inside an isolated revision. They do not yet
execute build commands; executable validation, failure-driven repair, durable
audit storage and the three assessment scenarios are delivered later.

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

$workflow | Select-Object id, status, contextRevision
$workflow.tasks | Format-Table id, type, status, attempts, approvalRequired
$workflow.changedFiles
$workflow.diff
```

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
  -Body '{"actor":"reviewer@example.com"}'
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
