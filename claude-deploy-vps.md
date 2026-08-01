# NoteFlow — VPS Deployment Runbook

Personalised deployment guide for **NoteFlow (IT4409)** onto an existing Ubuntu VPS
that already runs other Docker workloads.

| Item | Value |
|---|---|
| Repository | `https://github.com/cosmosthewill/IT4409-NoteApp` |
| Application directory | `/opt/note-app` |
| Compose project name | `note-app` |
| Application origin | `http://127.0.0.1:8080` |
| Public entrypoint | Cloudflare Tunnel (new tunnel, new subdomain) |
| Private administration | Tailscale SSH (already installed) |
| Lifetime | Temporary — deployed for a school report, then fully removed |

---

## Target host

The VPS is **not** a fresh machine. It already runs:

| Container | Image | Ports |
|---|---|---|
| `new-api` | `calciumion/new-api:latest` | `127.0.0.1:3000->3000/tcp` |
| `postgres` | `postgres:15` | `5432/tcp` (not published) |
| `redis` | `redis:latest` | `6379/tcp` (not published) |

Resources: 4436 MB RAM, ~4055 MB available, **no swap**, load ~0.00.

Docker and Tailscale are already installed, so the base-install steps of a
generic runbook are skipped.

### Why the existing stack is not disturbed

- Docker Compose namespaces every resource by project name. Our services become
  `note-app-postgres-1` and `note-app-note-app-1` — no collision with the
  existing container literally named `postgres`.
- Our containers sit on their own bridge network, `note-app_default`. They cannot
  reach, and are not reachable from, the existing stack.
- Our database volume is `note-app_note_app_data`, distinct from any volume used
  by `new-api`.
- Nothing new is published publicly. The app binds to `127.0.0.1:8080` only.

### Deliberate deviations from the generic runbook

- **Docker install steps are skipped** — Docker Engine and the Compose plugin are present.
- **Tailscale install is skipped** — already joined to the tailnet.
- **UFW is skipped entirely.** The host already runs working services, Cloudflare
  Tunnel is outbound-only, and Tailscale covers administration. Enabling a host
  firewall here adds risk to `new-api` without protecting anything.

---

## Rules

1. Never commit or push `.env`. It holds the database and demo-account passwords.
2. Create `.env` on the VPS after cloning. It is intentionally not in Git.
3. Do not publish PostgreSQL port `5432`.
4. Do not publish application port `8080`. Cloudflare Tunnel reaches `localhost:8080`.
5. Never paste the Cloudflare tunnel token into Git, chat, or this file.
6. `DEMO_PASSWORD=123456` is acceptable only because this is a short-lived graded demo.

---

## Architecture

```text
Visitor  ──HTTPS──>  Cloudflare Edge
                          │
                          │ outbound tunnel (no inbound port opened)
                          v
                  cloudflared (systemd, VPS host)
                          │
                          v
                http://localhost:8080         ← published on 127.0.0.1 only
                          │
                          v
              note-app-note-app-1  (Spring Boot 4.1, Java 21)
                          │  network: note-app_default
                          v
              note-app-postgres-1  (postgres:17-alpine)
                          │
                          v
              volume: note-app_note_app_data


Administrator ──> Tailscale tailnet ──> ssh ubuntu@100.x.x.x
```

---

## Phase A — Push the source to GitHub

*Run on: development machine*

Repository: <https://github.com/cosmosthewill/IT4409-NoteApp> (public).

The following files are deliberately **excluded** from the repository by `.gitignore`:

| Excluded | Reason |
|---|---|
| `.env` | Contains real passwords |
| `plan.md`, `report.md` | Working documents |
| `deploy.yaml`, `DEPLOY.md`, `claude-deploy-vps.md` | Deployment notes, not application source |
| `docs/FINAL_SUBMISSION.md` | Course submission outline |
| `*.pdf` | Course exam paper |
| `target/`, `.idea`, `.codegraph` | Build output and tool state |

```bash
cd "D:/Code/NoteApp-Duy/NoteApp"

git init
git add -A
git commit -m "Initial NoteFlow deployment"
git branch -M main
git remote add origin https://github.com/cosmosthewill/IT4409-NoteApp.git
git push -u origin main
```

### Verify before pushing

```bash
git ls-files .env      # must print nothing
git ls-files | wc -l   # expect 66 files
```

Expected result: 66 files — application source, `pom.xml`, the Maven wrapper,
`Dockerfile`, `compose.yaml`, `compose.dev.yaml`, `.env.example`, and `README.md`.

After pushing, open the repository on GitHub and confirm `.env` is absent.

---

## Phase B — VPS preflight

*Run on: VPS*

```bash
docker compose version          # expect v2.x
sudo ss -tlnp | grep 8080       # expect NO output — port must be free
tailscale status                # confirm this VPS is on the tailnet
df -h /                         # confirm several GB free for the build
free -m                         # confirm available memory
```

### Record a baseline before changing anything

This snapshot is the evidence used in Phase G to prove nothing belonging to the
existing stack was removed.

```bash
mkdir -p ~/note-app-baseline
docker ps -a --format '{{.Names}}\t{{.Image}}' | sort > ~/note-app-baseline/containers.txt
docker volume ls --format '{{.Name}}'           | sort > ~/note-app-baseline/volumes.txt
docker network ls --format '{{.Name}}'          | sort > ~/note-app-baseline/networks.txt
cat ~/note-app-baseline/volumes.txt
```

Read `volumes.txt` now and note the volume names belonging to `new-api`,
`postgres`, and `redis`. None of them start with `note-app_`.

If anything is already listening on `8080`, change the host side of the port
mapping in `compose.yaml` (for example to `127.0.0.1:8081:8080`) and use the new
port in the Cloudflare route in Phase E.

---

## Phase C — Clone and create `.env`

*Run on: VPS*

```bash
sudo mkdir -p /opt/note-app
sudo chown "$USER:$USER" /opt/note-app
git clone https://github.com/cosmosthewill/IT4409-NoteApp.git /opt/note-app
cd /opt/note-app
```

Generate a strong database password. Hex output is used so the value contains no
characters that the shell or Compose could interpret:

```bash
DB_PW=$(openssl rand -hex 16)
echo "DB_PASSWORD will be: $DB_PW"
```

```bash
cat > .env <<EOF
DB_NAME=note_app
DB_USERNAME=note_user
DB_PASSWORD=$DB_PW

DEMO_USERNAME=demo
DEMO_EMAIL=demo@example.com
DEMO_PASSWORD=123456

SPRING_PROFILES_ACTIVE=prod
DB_HOST=localhost
DB_PORT=5432
EOF

chmod 600 .env
```

Verify without printing secrets:

```bash
awk -F= '/^[A-Z_]+=/ {print $1 "=<set>"}' .env
```

Notes:

- This heredoc uses **unquoted** `EOF` on purpose, so `$DB_PW` expands. The generic
  runbook uses `<<'EOF'` because it writes literal passwords.
- `compose.yaml` overrides `DB_HOST` to `postgres` inside the container.
  `DB_HOST=localhost` only matters if the JAR is run outside Compose.
- `DEMO_PASSWORD` is left as `123456` so the instructor can log in easily. The
  database password is strong because changing it later requires an `ALTER ROLE`
  inside PostgreSQL, not merely an `.env` edit.
- `compose.yaml` uses `${DB_PASSWORD:?...}` and `${DEMO_PASSWORD:?...}`, so Compose
  refuses to start if either is missing. `.env` is mandatory.

---

## Phase D — Build, start, and verify the origin

*Run on: VPS, inside `/opt/note-app`*

```bash
cd /opt/note-app
sudo docker compose config --quiet
sudo docker compose up -d --build
sudo docker compose ps
```

The first build takes roughly 3–6 minutes: it pulls the Maven and Temurin images
and downloads all dependencies.

### If the build is killed for memory

The host has no swap. The Maven build peaks near 1.5 GB, which should fit in the
~4 GB available, but if the build is OOM-killed, add swap and retry:

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
free -m
```

### Verify

```bash
sudo docker compose logs --tail=100 note-app
curl -I http://127.0.0.1:8080
```

Expected:

- `postgres` reports healthy, `note-app` is `Up`
- The log shows the `prod` profile active
- The JDBC URL contains `postgres:5432/note_app`
- Tomcat starts on port 8080
- `curl` returns `HTTP/1.1 200`

> **Stop condition**
> Do not configure Cloudflare until `curl` returns 200 on the VPS.

Confirm isolation from the existing stack:

```bash
docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Ports}}'
docker volume ls | grep note
```

`new-api`, `postgres`, and `redis` must still be `Up` and unchanged.

---

## Phase E — New Cloudflare Tunnel on a new subdomain

*Run on: Cloudflare dashboard and VPS*

An existing tunnel serves a different hostname from an older VPS. That tunnel is
**not** touched. A new tunnel and a previously unused subdomain are created here,
so the old VPS can be destroyed at any time without affecting this deployment.

Cloudflare will not route one hostname through two tunnels, which is precisely why
a new subdomain is used instead of reusing the existing one.

### Dashboard steps

1. Cloudflare Dashboard → **Zero Trust** → **Networks** → **Tunnels**
2. **Create a tunnel** → **Cloudflared**
3. Name it `note-app-vps`
4. Environment: **Debian / Ubuntu**, **64-bit**
5. Copy the displayed install command and run it on the VPS. It looks like
   `sudo cloudflared service install eyJhIjoi...`.
   **That command contains a secret tunnel token — never commit or share it.**
   It is undone later by step 4.1 of Phase G, which removes the service and token
   but keeps the `cloudflared` binary for future tunnels.
6. Wait until the connector shows **Healthy**
7. **Public Hostname** → **Add a public hostname**
   - Subdomain: `notes` (must not already be in use)
   - Domain: your domain
   - Type: **HTTP**
   - URL: `localhost:8080`
8. Save

### Verify

```bash
sudo systemctl status cloudflared --no-pager
sudo journalctl -u cloudflared --no-pager -n 50
```

Expected: the service is active, Cloudflare shows the tunnel **Healthy**, and the
public hostname serves the NoteFlow landing page over HTTPS.

---

## Phase F — Acceptance test

*Run in: a browser outside the VPS*

- [ ] Open the public hostname over HTTPS
- [ ] Register a new account
- [ ] Log in as `demo` / `123456`
- [ ] Create, view, edit, pin, filter, search, and delete a note
- [ ] Switch between Vietnamese and English
- [ ] Switch between light and dark themes
- [ ] Check the mobile layout
- [ ] Confirm a second user cannot see the first user's notes

---

## Operations

### Status and logs

```bash
cd /opt/note-app
sudo docker compose ps
sudo docker compose logs -f note-app
```

### Deploy an update

```bash
cd /opt/note-app
git pull --ff-only
sudo docker compose up -d --build
curl -I http://127.0.0.1:8080
```

The named volume survives container recreation.

### Stop and restart without data loss

```bash
cd /opt/note-app
sudo docker compose down       # stop; volume preserved
sudo docker compose up -d      # start again
sudo docker compose restart    # restart in place
```

### Inspect the database with a GUI client (Navicat, DBeaver, pgAdmin)

`compose.yaml` publishes PostgreSQL as:

```yaml
    ports:
      - "${DB_BIND_ADDR:-127.0.0.1}:5433:5432"
```

The bind address is configurable per host and **defaults to `127.0.0.1`**, so a
fresh clone of the public repository never exposes the database beyond the machine
it runs on. Port `5433` is used on the host side to avoid confusion with the
conventional `5432`; inside the container it is still `5432`.

| `DB_BIND_ADDR` | Database reachable from |
|---|---|
| unset (default `127.0.0.1`) | the VPS itself only |
| `100.x.x.x` (Tailscale) | devices on your tailnet only |
| `0.0.0.0` | **the entire internet — never do this** |

#### Chosen setup: bind to the Tailscale address

This deployment binds to the VPS Tailscale address so GUI clients on the same
tailnet connect directly, with no SSH tunnel. Tailscale traffic is encrypted and
private, and the port remains invisible to the public internet.

Add the line to `/opt/note-app/.env` on the VPS — **not** to the repository:

```bash
cd /opt/note-app
tailscale ip -4                                  # note the 100.x.x.x address
echo "DB_BIND_ADDR=$(tailscale ip -4)" >> .env
sudo docker compose up -d
```

Verify the bind moved to the Tailscale interface:

```bash
sudo ss -tlnp | grep 5433
```

Expect `100.x.x.x:5433`. If it shows `0.0.0.0:5433`, stop immediately and correct
`DB_BIND_ADDR` — that value would expose the database publicly.

#### Navicat connection settings

Retrieve the password first:

```bash
grep '^DB_PASSWORD=' /opt/note-app/.env
```

**General tab** — the SSH, SSL, and HTTP tabs are all left untouched:

| Field | Value |
|---|---|
| Connection Name | anything, e.g. `Duy_note_apps` |
| Host | the VPS Tailscale address, `100.x.x.x` |
| Port | `5433` |
| Initial Database | `note_app` |
| User Name | `note_user` |
| Password | `DB_PASSWORD` from `/opt/note-app/.env` |

Common mistakes:

- **Port `5432`** — that is the port *inside* the container. The host publishes `5433`.
- **Initial Database `postgres`** — the application's database is `note_app`.
- **Enabling the SSH tab** — not needed with a Tailscale bind, and Tailscale SSH
  does not use conventional key authentication, so it tends to fail confusingly.

#### Alternative: keep the loopback default and tunnel over SSH

If `DB_BIND_ADDR` is left unset, reach the database with a manual tunnel instead:

```bash
ssh -N -L 5433:127.0.0.1:5433 ubuntu@100.x.x.x
```

Leave that running and point the client at `localhost:5433`. `-N` means "no remote
command", so the process only holds the tunnel open.

#### Schema reference

| Table | Notable columns |
|---|---|
| `users` | `id`, `username`, `email`, `password_hash`, `created_at`, `updated_at` |
| `notes` | `id`, `title`, `content`, `category`, `pinned`, `user_id`, `created_at`, `updated_at` |

`notes.user_id` is a foreign key to `users.id` (`fk_notes_user`). Passwords are
BCrypt hashes and cannot be reversed.

#### Terminal alternative, no tunnel required

```bash
cd /opt/note-app
sudo docker compose exec postgres psql -U note_user -d note_app
```

### Database backup

```bash
cd /opt/note-app
mkdir -p backups
sudo docker compose exec -T postgres pg_dump -U note_user note_app \
  > "backups/note_app-$(date +%F-%H%M%S).sql"
ls -lh backups
```

Copy backups off the VPS. A backup stored only on the VPS is not a backup.

---

## Phase G — Complete removal

Run this once the report has been graded. Every resource is namespaced under the
`note-app` Compose project, so removal is precise.

> **The only real hazard in this whole runbook**
> `docker compose down -v` acts on whichever project it finds in the **current
> directory**. Run it in the wrong folder and it deletes *that* stack's data.
> Every command below therefore names the project and file explicitly with
> `-p note-app -f /opt/note-app/compose.yaml`, which makes the working directory
> irrelevant. Do not substitute a bare `docker compose down -v`.

### 1. Confirm exactly what will be deleted

```bash
sudo docker compose -p note-app -f /opt/note-app/compose.yaml ps -a
docker volume ls | grep note-app
```

The first command must list only `note-app-note-app-1` and `note-app-postgres-1`.
The second must print only `note-app_note_app_data`. Volumes belonging to
`new-api`, `postgres`, and `redis` carry different names and are not matched.

### 2. Optional final backup

```bash
cd /opt/note-app
sudo docker compose exec -T postgres pg_dump -U note_user note_app > ~/note_app-final.sql
```

### 3. Remove containers, volume, image, and source

```bash
sudo docker compose -p note-app -f /opt/note-app/compose.yaml down -v --rmi local
sudo rm -rf /opt/note-app
```

`-v` removes only volumes declared in that specific file, namespaced to the
`note-app` project — that is, `note-app_note_app_data` and nothing else.
`--rmi local` removes the image built from the local `Dockerfile`; the shared
`postgres:17-alpine` base image is left in place.

### 4. Remove the tunnel connector

There are two different scopes here. Pick one — they are not the same thing.

#### 4.1 Remove only this tunnel, keep `cloudflared` installed — **recommended**

Use this if Cloudflare Tunnel will be used again on this VPS in the future.

The Phase E dashboard command was:

```text
sudo cloudflared service install eyJhIjoiXXXXXXXX...            ← long secret token
```

That command did two things: it installed the `cloudflared` binary, and it
registered a systemd service holding that token. The following removes **only the
service and its embedded token**, leaving the binary in place:

```bash
sudo cloudflared service uninstall
```

This stops and disables `cloudflared.service`, deletes its systemd unit, and takes
the tunnel token off the host. The `cloudflared` program itself stays installed
and fully usable.

Verify the split took effect as intended:

```bash
systemctl status cloudflared --no-pager   # expect: Unit cloudflared.service could not be found
which cloudflared                         # expect: a path — binary still present
cloudflared --version                     # expect: a version — still usable
```

To attach a **different** tunnel later, no reinstall is needed — just run the new
install command from that tunnel's dashboard page:

```bash
sudo cloudflared service install <NEW_TUNNEL_TOKEN>
```

#### 4.2 Remove `cloudflared` from the VPS entirely

Only if Cloudflare Tunnel will never be used on this host again:

```bash
sudo cloudflared service uninstall     # run this first, while the binary still exists
sudo apt remove -y cloudflared
```

Order matters. Removing the package first orphans the systemd unit, leaving a
failing service with no binary to uninstall it.

### 5. Clean up Cloudflare

Steps 4.1 and 4.2 only detach **this VPS** from the tunnel. The tunnel object and
its DNS record still exist in your Cloudflare account until removed in the
dashboard:

- Delete the `note-app-vps` tunnel (Zero Trust → Networks → Tunnels)
- Delete the `notes` DNS record (the site's DNS tab)

Your other tunnels, hostnames, and the domain itself are unaffected.

### 6. Remove the swapfile, if one was added

```bash
sudo swapoff /swapfile
sudo rm -f /swapfile
```

### 7. Verify the host is back to its original state

Compare against the baseline recorded in Phase B:

```bash
docker ps -a --format '{{.Names}}\t{{.Image}}' | sort | diff ~/note-app-baseline/containers.txt -
docker volume ls --format '{{.Name}}'          | sort | diff ~/note-app-baseline/volumes.txt -
docker network ls --format '{{.Name}}'         | sort | diff ~/note-app-baseline/networks.txt -
```

Each `diff` must print nothing. Empty output is proof that the host is byte-for-byte
back to its pre-deployment inventory: `new-api`, `postgres`, and `redis` still
running, and every one of their volumes intact.

```bash
rm -rf ~/note-app-baseline
```

---

## Security summary

| Item | Value |
|---|---|
| Public inbound ports opened | none |
| Ports that must stay unpublished | `8080`, `5432` |
| Cloudflare origin | `http://localhost:8080` |
| Database network | private `note-app_default` bridge |
| Secret file excluded from Git | `.env` |

Never share: `DB_PASSWORD`, the Cloudflare tunnel token, or any GitHub credential.
