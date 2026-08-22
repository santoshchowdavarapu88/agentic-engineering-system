# Architecture

## Product boundary

The product is the agentic software-engineering system. The URL shortener is a
controlled target repository used to prove that the system can transform a
requirement into a generated, applied, executed and reviewable engineering
outcome. Workflow execution never edits the submitted fixture in place.

## Agentic control loop

```text
Requirement
    |
    v
Requirement agent -- ambiguity --> AWAITING_CLARIFICATION -- human answer --+
    |                                                                       |
    v                                                                       |
Isolated repository copy <-------------------------------------------------+
    |
    v
Repository analysis -> architecture plan -> implementation proposal
                                             |               |
                                             |               +-> documentation
                                             v
                                      test proposal
                                             |
                                             v
                                  policy-checked patch apply
                                             |
                                             v
                                      Maven clean test
                                       /           \
                                  success         failure evidence
                                     |                  |
                                     |            repair agent
                                     |                  |
                                     |        rollback + corrected patch
                                     |                  |
                                     +<----------- bounded retry
                                     |
                                     v
                              human approval gate
```

This is agentic because observed state changes the execution path. Ambiguity
prevents implementation, repository evidence grounds the generated plan, and a
real build failure becomes structured repair input rather than merely a failed
HTTP response.

## Responsibility split

The deterministic Java orchestrator owns workflow state, dependency ordering,
entry and exit gates, workspace isolation, retry limits, command selection,
timeouts, path policy, rollback and approval. Logical agents own requirement
interpretation, repository-specific planning, source generation, test
generation, repair and documentation. `EngineeringModel` allows those agents
to use either the deterministic provider or the OpenAI Responses API without
moving control-policy decisions into the model.

## Trust boundaries

- Requirements, repository text and model output are untrusted input.
- Repository paths must remain relative to an approved configured root.
- Each workflow operates on `agent-workspaces/<id>/revision-<n>/repository`.
- Model proposals cannot directly access the filesystem or launch processes.
- Patch paths, operation types, duplicate paths, extensions, sizes and expected
  SHA-256 values are validated before mutation.
- Maven is exposed as the fixed `MAVEN_TEST` capability; the model cannot supply
  shell text or command arguments.
- Model credentials are removed from the validation process environment.
- Validation duration and captured output are bounded.
- Failed repair attempts restore the immutable baseline before applying the
  complete corrected proposal.
- Human approval is required only after executable evidence exists.
- Every approval and safe stop requires an identified actor and reason.
- Audit events are append-only PostgreSQL records with correlation IDs.

## Validation and repair evidence

Each attempt records a bounded build log in `logs/maven-test-attempt-N.log` and
a concise report in `artifacts/validation-report-attempt-N.md`. The workflow API
returns the final changed-file list, SHA-based diff evidence, attempt count and
whether repair occurred. Exhausted attempts fail the workflow and block release
readiness.

## Scenario strategy

- Greenfield starts from a README-only repository and generates its Maven build,
  URL-shortener production code and tests.
- Brownfield inspects an existing Spring Boot URL shortener and generates
  redirect analytics components in the repository's package conventions.
- Ambiguous stops after requirement analysis, accepts human clarification and
  only then expands and executes the downstream graph.

Durable audit events feed Micrometer counters and timers for workflow outcomes,
task retries, repairs, rollbacks, total latency and repair recovery time. The
Prometheus endpoint exposes these as `agentic_*` series.

## Current limitations

- Active workflow state is in memory and cannot yet resume after an application
  restart. Audit and governance lineage is durable in PostgreSQL.
- The deterministic provider intentionally produces a bounded generated change;
  richer URL-analytics output is added by the scenario harness.
- Maven is the only executable build capability currently exposed.
- The platform produces a reviewed workspace outcome, not a direct commit or
  deployment to an external repository.
