# Consolidated Engineering Outcome

## Objective and rationale

The deliverable is an agentic software-engineering system, not merely a URL
shortener. A deterministic orchestrator owns state, dependencies, effects and
policy. Specialized agents interpret requirements and propose structured
engineering decisions through a provider-neutral model boundary. This allows
LLM reasoning without granting the model unrestricted filesystem or commands.

## Delivered plan

1. Interpret the requirement and pause when clarification is required.
2. Snapshot an approved repository into a revision-isolated workspace.
3. Analyze the repository and build a dependency-aware execution graph.
4. Generate architecture, implementation, tests and documentation proposals.
5. Validate and atomically apply bounded, optimistic-locking file changes.
6. Run a fixed Maven capability and retain complete validation evidence.
7. Restore, diagnose, repair and revalidate within a bounded retry policy.
8. Authenticate distinct operator/approver roles, stop at human approval and
   retain durable decision lineage and workflow checkpoints.

## Outputs and evidence

Each workflow exposes its graph, context revision, changed files and diff. The
workspace retains analysis, plans, build logs and validation reports. PostgreSQL
retains ordered audit events with correlation and task lineage. Prometheus
exposes workflow, task, retry, repair, rollback, latency and recovery metrics.

## Assessment scenarios

- **Greenfield:** generates a Java 21 URL-shortener build, source and tests from
  a README-only repository, then executes Maven validation.
- **Brownfield:** analyzes an existing Spring Boot service, adds total/daily UTC
  redirect analytics, applies source and tests, and validates the repository.
- **Ambiguous:** pauses after analysis, requests clarification, dynamically
  expands the graph after an answer, then generates and validates the outcome.

Exact API steps and expected evidence are in `REVIEWER-GUIDE.md`.

## Assumptions

- Inputs are Maven repositories or requirements requesting a generated project.
- Repository inputs remain below the configured approved root.
- Deterministic mode is the credential-free, reproducible reviewer path.
- OpenAI mode uses an external key and the same structured agent contracts.
- A human reviews diff, evidence and risks before release approval.

## Risks and controls

| Risk | Control |
|---|---|
| Path escape | Approved roots, normalized safe paths and bounded reads |
| Unauthorized mutation | Structured patches, preflight policy and isolated revisions |
| Stale overwrite | SHA-256 optimistic locking |
| Partial application | Immutable baseline and verified rollback |
| Arbitrary execution | Fixed Maven capability, timeout and stripped credentials |
| Broken generated code | Executable tests, logs and evidence-driven repair |
| Unauthorized governance action | Stateless authentication and distinct operator/approver roles |
| Spoofed approval identity | Actor derived from the authenticated principal |
| Infinite autonomy | Attempt limits, safe stop and approval gates |
| Secret disclosure | Environment credentials and model-command isolation |

## Known limitations

- Active workflow execution is in memory. Audit history and state checkpoints
  are durable, but interrupted execution does not automatically resume.
- Validation currently supports Maven, not multiple ecosystems or hardened
  remote execution sandboxes.
- Deterministic mode demonstrates bounded scenarios; general repository changes
  require the configured LLM provider.
- OpenAI behavior depends on model availability and supplied credentials.
- Production needs authentication, encrypted secrets, quotas, external artifact
  storage and stronger network isolation.

## Verification and delivery

`mvn clean verify` runs tests, creates a JaCoCo report and enforces line coverage.
GitHub Actions repeats verification and builds the non-root container. Compose
provides the application, PostgreSQL, health checks and persistent storage.

## Provider evidence

Deterministic mode is a repeatable test double and bounded demonstration fixture;
it proves orchestration and safety behavior, not open-ended reasoning. OpenAI mode
is the general agentic path for requirement interpretation, repository-grounded
planning, code/test generation and repair. Its API boundary is tested with a
local mock server, so CI requires neither credentials nor paid calls.
