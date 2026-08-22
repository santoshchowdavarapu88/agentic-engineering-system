# Reviewer Guide

This guide demonstrates the three assessment scenarios using deterministic mode,
so no paid model credential or network model call is required. The same logical
agents use the OpenAI provider when `MODEL_PROVIDER=openai`.

## Start

```powershell
docker compose up -d postgres
$env:MODEL_PROVIDER = "deterministic"
.\mvnw.cmd spring-boot:run
```

In another terminal:

```powershell
$base = "http://localhost:8080/api/v1"
$headers = @{ "X-Correlation-ID" = "reviewer-scenarios" }
Invoke-RestMethod "$base/engineering-scenarios" | Format-Table id, scenarioType, expectedPause
```

## 1. Greenfield

The input repository contains only a README. The agents must generate the build,
production source and tests before Maven validation.

```powershell
$greenfield = Invoke-RestMethod -Method Post -Uri "$base/engineering-workflows" `
  -Headers $headers -ContentType "application/json" -Body (@{
    scenarioType = "GREENFIELD"
    requirement = "Greenfield: create a Java 21 URL shortener with collision-safe codes and unit tests"
    repositoryPath = "greenfield-url-shortener"
  } | ConvertTo-Json)

$greenfield | Select-Object id, status, validationAttempts, repaired, changedFiles
```

Expected: `AWAITING_APPROVAL`, one validation attempt, and generated `pom.xml`,
URL-shortener source and tests. This proves project-level output generation from
an empty engineering repository.

## 2. Brownfield

The input is an existing Spring Boot URL shortener. Repository analysis grounds
generation in its package and MVC conventions.

```powershell
$brownfield = Invoke-RestMethod -Method Post -Uri "$base/engineering-workflows" `
  -Headers $headers -ContentType "application/json" -Body (@{
    scenarioType = "BROWNFIELD"
    requirement = "Add total and daily UTC redirect analytics with a read-only REST endpoint"
    repositoryPath = "url-shortener"
  } | ConvertTo-Json)

$brownfield | Select-Object id, status, validationAttempts, repaired, changedFiles
```

Expected artifacts include an analytics service, REST controller, successful-
redirect interceptor, MVC configuration and deterministic UTC aggregation test.
The copied repository is patched and `mvn clean test` must succeed before the
workflow can reach approval.

## 3. Ambiguous and replanned

```powershell
$ambiguous = Invoke-RestMethod -Method Post -Uri "$base/engineering-workflows" `
  -Headers $headers -ContentType "application/json" -Body (@{
    scenarioType = "AMBIGUOUS"
    requirement = "Improve analytics"
    repositoryPath = "url-shortener"
  } | ConvertTo-Json)

$ambiguous.status
$ambiguous.tasks | Format-Table type, status
```

Expected: `AWAITING_CLARIFICATION` and only requirement analysis has run. No
patch or build task is allowed to execute.

```powershell
$ambiguous = Invoke-RestMethod -Method Post `
  -Uri "$base/engineering-workflows/$($ambiguous.id)/clarification" `
  -Headers $headers -ContentType "application/json" -Body (@{
    clarification = "Track total redirects per short code and UTC calendar day; expose a read-only REST endpoint."
  } | ConvertTo-Json)

$ambiguous | Select-Object status, validationAttempts, changedFiles
```

Expected: the orchestrator generates the downstream DAG only after clarification,
produces the analytics artifacts, validates them and stops at approval.

## Decomposition and lineage

For any workflow:

```powershell
$id = $brownfield.id
Invoke-RestMethod "$base/engineering-workflows/$id" |
  Select-Object -ExpandProperty tasks |
  Format-Table id, type, status, dependencies, attempts, approvalRequired

Invoke-RestMethod "$base/engineering-workflows/$id/governance/audit-events" |
  Format-Table sequence, type, taskId, correlationId, occurredAt
```

The lineage shows requirement analysis, plan generation, repository reasoning,
source/test generation, policy-checked application, validation and approval.

## Evidence and approval

```powershell
$workflowRoot = ".\agent-workspaces\$id\revision-1"
Get-ChildItem "$workflowRoot\artifacts"
Get-ChildItem "$workflowRoot\logs"

$release = $brownfield.tasks | Where-Object approvalRequired | Select-Object -First 1
Invoke-RestMethod -Method Post `
  -Uri "$base/engineering-workflows/$id/tasks/$($release.id)/approval" `
  -Headers $headers -ContentType "application/json" `
  -Body '{"actor":"reviewer@example.com","reason":"Diff, risks and executable evidence reviewed"}'
```

## Metrics

```powershell
(Invoke-WebRequest "http://localhost:8080/actuator/prometheus").Content |
  Select-String "agentic_"
```

Metrics cover workflow outcomes and latency, task outcomes and retries, repairs,
rollbacks, event types and repair recovery time.

## What remains controlled

The model cannot choose shell commands, repository roots, retry counts, patch
limits or approval policy. It proposes structured engineering decisions; the
deterministic orchestrator owns effects and governance.
