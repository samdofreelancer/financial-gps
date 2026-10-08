# OpenCode port conflict

## Symptom

Starting OpenCode fails with a message similar to:

> Managed service port 49374 on 127.0.0.1 is already in use by another process.

## Root cause

Another process is already listening on the configured OpenCode service port.

## Resolution

1. Choose a free port.
2. Reconfigure OpenCode to use the new port.
3. Restart the service.

### Example command

```bash
opencode service set port 50000
opencode
```

## Quick port check

```bash
lsof -i :49374
```

If the command returns a PID, that process is occupying the port.

```bash
kill -9 <PID>
```

## Notes

- Prefer a high ephemeral port such as 50000+ when local tooling is involved.
- Keep this note here so future debugging does not require searching through terminal history.
