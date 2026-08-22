# URL Shortener Scenario Repository

This controlled brownfield fixture is a runnable Spring Boot URL shortener. It
contains create and redirect behavior plus baseline unit tests. The agentic
platform copies it into a revision workspace, generates source and test changes,
applies them under policy, and runs `mvn clean test` against the changed copy.

The fixture is evidence for the engineering platform; it is not the primary
product and is never modified in place by a workflow.
