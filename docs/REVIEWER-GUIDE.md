# Reviewer Guide

This guide uses deterministic mode for reproducible, credential-free verification
of orchestration, controls and executable validation. It is intentionally a
bounded scenario generator, not evidence of general model reasoning. For genuine
requirement interpretation and repository-specific generation, run the same
logical agents with `MODEL_PROVIDER=openai`; only the model adapter changes.

## Start

```powershell
$env:AGENT_OPERATOR_USERNAME = "operator"
$env:AGENT_OPERATOR_PASSWORD = "local-operator-password"
$env:AGENT_APPROVER_USERNAME = "approver"
$env:AGENT_APPROVER_PASSWORD = "local-approver-password"
docker compose up -d postgres
$env:MODEL_PROVIDER = "deterministic"
.\mvnw.cmd spring-boot:run
```

In another terminal:

```powershell
$base = "http://localhost:8080/api/v1"
$operatorToken = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("operator:local-operator-password"))
$approverToken = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("approver:local-approver-password"))
$operatorHeaders = @{ Authorization = "Basic $operatorToken"; "X-Correlation-ID" = "reviewer-scenarios" }
$approverHeaders = @{ Authorization = "Basic $approverToken"; "X-Correlation-ID" = "reviewer-scenarios" }
Invoke-RestMethod "$base/engineering-scenarios" -Headers $operatorHeaders | Format-Table id, scenarioType, expectedPause
```

## 1. Greenfield

The input repository contains only a README. The agents must generate the build,
production source and tests before Maven validation.

```powershell
$greenfield = Invoke-RestMethod -Method Post -Uri "$base/engineering-workflows" `
  -Headers $operatorHeaders -ContentType "application/json" -Body (@{
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
  -Headers $operatorHeaders -ContentType "application/json" -Body (@{
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
  -Headers $operatorHeaders -ContentType "application/json" -Body (@{
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
  -Headers $operatorHeaders -ContentType "application/json" -Body (@{
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
Invoke-RestMethod "$base/engineering-workflows/$id" -Headers $operatorHeaders |
  Select-Object -ExpandProperty tasks |
  Format-Table id, type, status, dependencies, attempts, approvalRequired

Invoke-RestMethod "$base/engineering-workflows/$id/governance/audit-events" -Headers $operatorHeaders |
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
  -Headers $approverHeaders -ContentType "application/json" `
  -Body '{"reason":"Diff, risks and executable evidence reviewed"}'
```

The ledger records the authenticated approver identity. Inspect the durable
workflow checkpoint independently of the in-memory execution aggregate:

```powershell
Invoke-RestMethod "$base/engineering-workflows/$id/governance/snapshot" `
  -Headers $operatorHeaders | Format-List
```

## Metrics

```powershell
(Invoke-WebRequest "http://localhost:8080/actuator/prometheus" -Headers $operatorHeaders).Content |
  Select-String "agentic_"
```

Metrics cover workflow outcomes and latency, task outcomes and retries, repairs,
rollbacks, event types and repair recovery time.

## What remains controlled

Anonymous requests are rejected; operators cannot approve releases and approvers
cannot create or safe-stop workflows. The model cannot choose shell commands,
repository roots, retry counts, patch
limits or approval policy. It proposes structured engineering decisions; the
deterministic orchestrator owns effects and governance.

## POSIX shell quick check

Linux and macOS reviewers can run the same brownfield path with `curl`:

```bash
export AGENT_OPERATOR_USERNAME=operator
export AGENT_OPERATOR_PASSWORD=local-operator-password
export AGENT_APPROVER_USERNAME=approver
export AGENT_APPROVER_PASSWORD=local-approver-password
export MODEL_PROVIDER=deterministic
docker compose up -d postgres
./mvnw spring-boot:run
```

In another shell:

```bash
BASE=http://localhost:8080/api/v1
curl -u operator:local-operator-password "$BASE/engineering-scenarios"
curl -u operator:local-operator-password -H 'Content-Type: application/json' \
  -H 'X-Correlation-ID: posix-review' -d '{
    "scenarioType":"BROWNFIELD",
    "requirement":"Add total and daily UTC redirect analytics",
    "repositoryPath":"url-shortener"
  }' "$BASE/engineering-workflows"
curl -u operator:local-operator-password http://localhost:8080/actuator/prometheus
```

Use returned workflow and release-task IDs with the endpoints above. Release
approval must use `-u approver:local-approver-password`.
