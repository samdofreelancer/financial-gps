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
  architecture/
    README.md
    overview.md
    system-context.md
  development/
    local-setup.md
    contributing.md
  operations/
    ports-and-services.md
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

- [architecture/overview.md](architecture/overview.md)
- [development/local-setup.md](development/local-setup.md)
- [operations/ports-and-services.md](operations/ports-and-services.md)
- [troubleshooting/README.md](troubleshooting/README.md)
- [decisions/README.md](decisions/README.md)
