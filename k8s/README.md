# Kubernetes manifests — financial-gps

Maps 1:1 to `compose.yaml`: **postgres** (StatefulSet) → **backend** (Spring Boot)
→ **frontend** (nginx SPA, proxies `/api/` to the backend).

## Layout

| File | Contents |
|---|---|
| `namespace.yaml` | `financialgps` namespace |
| `configmap.yaml` | Non-sensitive config (`DB_NAME`, `DB_USER`, Spring settings) |
| `secret.example.yaml` | Template only — documents the expected secret, never applied |
| `postgres.yaml` | StatefulSet + headless Service `postgres` + 5 Gi PVC |
| `backend.yaml` | Deployment (2 replicas) + Service `backend:8080` |
| `frontend.yaml` | Deployment (2 replicas) + Service `frontend:80` |
| `ingress.yaml` | Ingress → frontend (TLS block commented) |
| `kustomization.yaml` | Ties it together; image tags overridable here |

## DNS contract (mirrors compose)

- Backend connects to `jdbc:postgresql://postgres:5432/...` → Service **postgres**
- Frontend nginx has `proxy_pass http://backend:8080;` hardcoded → Service **backend**
- Keep these Service names (or update the config) when renaming.

## Deploy

```bash
# 1. Build images (or push to your registry and set tags in kustomization.yaml)
docker build -t financialgps/backend:local  backend/
docker build -t financialgps/frontend:local frontend/
# kind/minikube only — make local images visible to the cluster:
# kind load docker-image financialgps/backend:local financialgps/frontend:local

# 2. Create the database secret (no default password exists by design)
kubectl create secret generic financialgps-db \
  --namespace financialgps \
  --from-literal=DB_PASSWORD='<strong-password>'

# 3. Apply everything
kubectl apply -k k8s/

# 4. Watch rollout
kubectl -n financialgps get pods -w
```

## Access

### Port-forward (quickest — no Ingress needed)

```bash
kubectl -n financialgps port-forward svc/frontend 3000:80
# → open http://localhost:3000
```

Syntax: `port-forward svc/frontend <LOCAL-PORT>:<SERVICE-PORT>`.
The left port is your machine (pick any free port), the right port is the
frontend Service (nginx, always 80).

### Port cheat-sheet

| Port | What it is |
|---|---|
| `3000` | Your machine → browser (arbitrary; any free local port works) |
| `80` | **frontend** Service/container port (nginx) |
| `8080` | **backend** Service/container port (Spring Boot) — cluster-internal only; nginx proxies `/api/*` to it |
| `5432` | **postgres** — cluster-internal only |

> ⚠️ **Browser login over HTTP:** the non-`local` Spring profile sets
> `financial.auth.cookie.secure=true`, so browsers only send the session cookie
> over HTTPS. The UI loads fine via port-forward, but login will not persist.
> For click-around local testing, either enable TLS (below) or run a dev
> overlay with the `local` profile (`cookie.secure=false`).

### Ingress (real deployments)

Set a real `host` and enable the commented `tls:` block in `ingress.yaml`
(e.g. with cert-manager), then browse to that host.

## Verify the deployment

```bash
# Pods — expect 2 backend, 2 frontend, 1 postgres, all 1/1 Running
kubectl -n financialgps get pods

# End-to-end check from inside the cluster
kubectl -n financialgps run verify --rm -i --image=curlimages/curl --restart=Never --quiet -- \
  sh -c "curl -s http://backend:8080/actuator/health; \
         curl -s -o /dev/null -w '%{http_code}\n' http://frontend/; \
         curl -s -o /dev/null -w '%{http_code}\n' http://frontend/api/auth/me"
# Expected: {"status":"UP",...}  /  200  /  401 (401 = proxy works, no session)

# Migrations ran?
kubectl -n financialgps logs deploy/backend | grep -i flyway | head
```

## Production notes

- **TLS is required.** The non-`local` Spring profile sets
  `financial.auth.cookie.secure=true`; session cookies are only sent over HTTPS.
  Enable the commented `tls:` block in `ingress.yaml` (e.g. with cert-manager).
- **Migrations:** Flyway runs on backend startup. Its database lock table makes
  rolling updates with 2 replicas safe — one pod migrates, the other waits.
- **Sessions** are stored in the database (Spring Session JDBC), so backend
  replicas are stateless and can scale freely (`kubectl scale deploy/backend -n financialgps --replicas=3`).
- **Backups:** the postgres PVC is your only stateful data — snapshot it via your
  StorageClass tooling or `pg_dump` from a CronJob.
- **Probes:** backend gets up to 5 minutes to start (JVM + Flyway) via a
  `startupProbe`, matching the 180 s `start_period` in compose.
- The **e2e** stack (Playwright) is intentionally not deployed — it belongs to CI.

## Redeploy after code changes

```bash
docker build -t financialgps/backend:local backend/     # and/or frontend/
kubectl -n financialgps rollout restart deployment/backend
kubectl -n financialgps rollout status  deployment/backend
```

## Troubleshooting

- **`./mvnw: not found` during `docker build`** — CRLF line endings from a
  Windows checkout break the script's shebang. Fixed permanently by
  `mvnw text eol=lf` in `.gitattributes` plus a `tr -d '\r'` guard in
  `backend/Dockerfile`. For existing checkouts: `git add --renormalize mvnw`.
- **Backend pods stuck `0/1` for a few minutes** — normal: JVM start + Flyway
  migrations; the `startupProbe` allows up to 5 min. Follow along with
  `kubectl -n financialgps logs -f deploy/backend`.
- **`CreateContainerConfigError` on backend/postgres** — the DB secret is
  missing: `kubectl create secret generic financialgps-db -n financialgps
  --from-literal=DB_PASSWORD='<strong-password>'`.
- **API returns 401 in the browser** — expected when not logged in; 401 from
  `/api/auth/me` confirms nginx → backend proxying works.
- **Login doesn't stick over plain HTTP** — see the secure-cookie note in
  *Access* above.
