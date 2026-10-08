# Project Documentation

This directory is the canonical knowledge base for the project.

## Goals

- preserve architecture and design knowledge
- document operational procedures and recovery steps
- reduce onboarding time for new contributors
- keep decisions and troubleshooting steps durable over time
- provide a consistent source of truth for both humans and AI agents

## Structure

```text
docs/
  README.md
  project-handbook.md
  architecture/
    README.md
    overview.md
    business-domain.md
    feature-map.md
    system-context.md
    security.md
    deployment.md
  development/
    local-setup.md
    contributing.md
    testing.md
  operations/
    README.md
    ports-and-services.md
  runbooks/
    README.md
    local-development.md
    docker-compose.md
  decisions/
    README.md
    adr-0001-project-structure-and-domain-first-design.md
  troubleshooting/
    README.md
    opencode-port-conflict.md
  onboarding/
    README.md
  glossary/
    README.md
```

## Standards

- docs should answer the question: “what is the system, how does it run, and how do we fix it?”
- keep language clear and practical
- prefer long-lived, reusable knowledge over chat-history style notes
- update docs whenever behavior or environment changes

## Start here

- [project-handbook.md](project-handbook.md)
- [architecture/overview.md](architecture/overview.md)
- [architecture/business-domain.md](architecture/business-domain.md)
- [architecture/security.md](architecture/security.md)
- [architecture/deployment.md](architecture/deployment.md)
- [development/local-setup.md](development/local-setup.md)
- [development/testing.md](development/testing.md)
- [operations/README.md](operations/README.md)
- [runbooks/local-development.md](runbooks/local-development.md)
- [runbooks/docker-compose.md](runbooks/docker-compose.md)
- [troubleshooting/README.md](troubleshooting/README.md)
- [decisions/README.md](decisions/README.md)
