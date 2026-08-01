# NoteFlow — Production Deployment Runbook

Manual deployment runbook for a **fresh Ubuntu 22.04 / 24.04 VPS**.

| Item | Value |
|---|---|
| Application directory | `/opt/note-app` |
| Application origin | `http://127.0.0.1:8080` |
| Public entrypoint | Cloudflare Tunnel |
| Private administration | Tailscale SSH |

> **Note**
> This is documentation, not a Docker Compose file. The stack itself is defined in
> [`compose.yaml`](compose.yaml).

## Official documentation

- Docker on Ubuntu — https://docs.docker.com/engine/install/ubuntu/
- Docker Linux post-install — https://docs.docker.com/engine/install/linux-postinstall/
- Cloning a GitHub repository — https://docs.github.com/en/repositories/creating-and-managing-repositories/cloning-a-repository
- Tailscale on Linux — https://tailscale.com/docs/install/linux
- Cloudflare Tunnel (remote-managed) — https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/get-started/create-remote-tunnel/

## Rules

1. Never commit or push `.env`. It contains the database and demo-account passwords.
2. Create `.env` separately on every deployment host, after cloning the repository.
3. Do not expose PostgreSQL port `5432` to the Internet.
4. Do not expose application port `8080` publicly. Cloudflare Tunnel reaches `localhost:8080`.
5. Keep the original SSH session open until Tailscale SSH has been tested successfully.
6. The password `123456` is suitable only for a temporary school demonstration.

## Values to replace

| Placeholder | Example |
|---|---|
| `GITHUB_REPOSITORY_URL` | `https://github.com/YOUR_USERNAME/YOUR_REPOSITORY.git` |
| `PUBLIC_HOSTNAME` | `notes.example.com` |
| `CLOUDFLARE_TUNNEL_NAME` | `note-app-vps` |
| `YOUR_VPS_PUBLIC_IP` | the provider-assigned IPv4 address |
| `YOUR_VPS_USER` | `root`, or the account supplied by the provider |

---

## Architecture

### Request flow

```text
Visitor  ──HTTPS──>  Cloudflare Edge
                          │
                          │ outbound tunnel
                          v
                    cloudflared (VPS)
                          │
                          v
              http://localhost:8080   ← Docker publishes only on 127.0.0.1:8080
                          │
                          v
                 note-app container
                          │ private Compose network
                          v
                 postgres container
```

### Administration flow

```text
Administrator ──> same Tailscale tailnet ──> ssh YOUR_VPS_USER@100.x.x.x
```

---

## Local preparation

Run once on the development machine. The project directory is not yet a Git repository.

```bash
git init
git add .
git status
git commit -m "Initial NoteFlow deployment"
git branch -M main
git remote add origin GITHUB_REPOSITORY_URL
git push -u origin main
```

### Verify that `.env` is not tracked

```bash
git check-ignore .env
git ls-files .env
```

Expected:

- `git check-ignore .env` prints `.env`
- `git ls-files .env` prints nothing

If `git ls-files .env` prints the file, it was tracked before the ignore rule was added.
Remove only the tracked copy and keep the local file:

```bash
git rm --cached .env
git commit -m "Stop tracking .env"
```

Use a **private** GitHub repository if the source must not be public.

---

## Fresh VPS deployment

### Step 1 — Connect to the new VPS

*Run on: development machine*

```bash
ssh root@YOUR_VPS_PUBLIC_IP
```

- Use the username supplied by the VPS provider if it is not `root`.
- Prefer an SSH key. Do not store the VPS password in this repository.

### Step 2 — Update Ubuntu and install base tools

*Run on: VPS*

```bash
sudo apt update
sudo apt upgrade -y
sudo apt install -y ca-certificates curl git
```

### Step 3 — Remove conflicting packages

*Run on: VPS*

```bash
sudo apt remove -y docker.io docker-compose docker-compose-v2 docker-doc podman-docker containerd runc
```

It is normal for `apt` to report that some or all of these packages are not installed.

### Step 4 — Add the official Docker apt repository

*Run on: VPS*

```bash
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

. /etc/os-release
sudo tee /etc/apt/sources.list.d/docker.sources >/dev/null <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: ${UBUNTU_CODENAME:-$VERSION_CODENAME}
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF

sudo apt update
```

The closing `EOF` must start at column 1 with no trailing spaces.

### Step 5 — Install and verify Docker Engine and the Compose plugin

*Run on: VPS*

```bash
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
sudo docker version
sudo docker compose version
sudo docker run --rm hello-world
```

Every deployment command below uses `sudo`, so joining the `docker` group is optional.

### Step 6 — Install Tailscale *before* changing firewall access

*Run on: VPS*

```bash
curl -fsSL https://tailscale.com/install.sh | sh
sudo tailscale up --ssh
tailscale ip -4
tailscale status
```

Expected:

- `tailscale up` prints an authentication URL
- After authentication the VPS appears on the Tailscale **Machines** page
- `tailscale ip -4` prints a `100.x.x.x` address

### Step 7 — Verify Tailscale access

*Run on: development machine, joined to the same tailnet*

```bash
ssh YOUR_VPS_USER@100.x.x.x
```

> **Warning**
> Do not close the original public-IP SSH session until this succeeds.

### Step 8 — Clone the application into `/opt`

*Run on: VPS*

```bash
sudo mkdir -p /opt/note-app
sudo chown "$USER:$USER" /opt/note-app
git clone GITHUB_REPOSITORY_URL /opt/note-app
cd /opt/note-app
```

For a private repository, configure an SSH deploy key or authenticate with an
appropriately scoped GitHub credential. Do not place a GitHub token in this file.

### Step 9 — Create `.env` directly on the VPS

*Run on: VPS, inside `/opt/note-app`*

`.env` is intentionally excluded from GitHub, so it must be created after every fresh
clone. The quoted `<<'EOF'` prevents the shell from expanding characters inside the
password values.

```bash
cd /opt/note-app

cat > .env <<'EOF'
DB_NAME=note_app
DB_USERNAME=note_user
DB_PASSWORD=123456

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
test -f .env
awk -F= '/^[A-Z_]+=/ {print $1 "=<set>"}' .env
```

Notes:

- The closing `EOF` must begin at column 1, with no spaces after it.
- `compose.yaml` overrides `DB_HOST` with `postgres` inside the application container.
  `DB_HOST=localhost` matters only if the JAR is run directly on the VPS, outside Compose.
- Replace both `123456` values before using this as a long-lived public service.

### Step 10 — Validate and start the production Compose stack

*Run on: VPS, inside `/opt/note-app`*

```bash
cd /opt/note-app
sudo docker compose config --quiet
sudo docker compose up -d --build
sudo docker compose ps
```

Expected:

- The `postgres` service becomes healthy
- The `note-app` service is `Up`
- PostgreSQL has no public host-port mapping
- The application is bound only to `127.0.0.1:8080`

### Step 11 — Inspect startup logs and test the local origin

*Run on: VPS*

```bash
cd /opt/note-app
sudo docker compose logs --tail=200 note-app
curl -I http://127.0.0.1:8080
```

Expected:

- The log reports that the `prod` profile is active
- The JDBC URL contains `postgres:5432/note_app`
- Tomcat starts on port 8080
- `curl` returns `HTTP/1.1 200`

> **Stop condition**
> Do not configure Cloudflare until `localhost:8080` works correctly on the VPS.

### Step 12 — Configure the Cloudflare Tunnel

*Run on: Cloudflare dashboard and VPS*

Prerequisites:

- The domain is active in the same Cloudflare account
- The application already returns HTTP 200 on `http://127.0.0.1:8080`

Dashboard steps:

1. Open the Cloudflare dashboard.
2. Go to **Networking → Tunnels**.
3. Select **Create tunnel**, then choose **Cloudflared**.
4. Name the tunnel `note-app-vps`, or another descriptive name.
5. Select **Linux** and the VPS architecture.
6. Copy the `cloudflared` installation and service-install command shown by Cloudflare,
   then run it on the VPS. **That command contains a secret tunnel token — never commit
   or share it.**
7. Wait until the connector status is **Healthy**.
8. Open the tunnel, select **Routes**, then **Add route → Published application**.
9. Choose the subdomain and domain, for example `notes.example.com`.
10. Select **HTTP** as the service type.
11. Enter `http://localhost:8080` as the service URL.
12. Save the published application route.

Verify on the VPS:

```bash
sudo systemctl status cloudflared --no-pager
sudo journalctl -u cloudflared --no-pager -n 100
```

Expected:

- The `cloudflared` service is active
- Cloudflare shows the tunnel as **Healthy**
- `https://PUBLIC_HOSTNAME` displays the NoteFlow landing page

### Step 13 — Apply a minimal host firewall policy

*Run on: VPS, only after Tailscale access has been verified*

```bash
sudo apt install -y ufw
sudo ufw allow OpenSSH
sudo ufw enable
sudo ufw status verbose
```

- Do not add public allow rules for ports `8080` or `5432`.
- Cloudflare Tunnel initiates an outbound connection; it does not require public inbound
  port 8080.
- Keep provider-console access available in case an SSH or firewall rule is incorrect.

### Step 14 — Final acceptance test

*Run in: a browser outside the VPS*

- [ ] Open `https://PUBLIC_HOSTNAME`
- [ ] Register a new account
- [ ] Log in with `demo` / `123456`
- [ ] Create, view, edit, pin, filter, search, and delete a note
- [ ] Verify Vietnamese and English
- [ ] Verify light and dark themes
- [ ] Verify the mobile layout
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
sudo docker compose ps
curl -I http://127.0.0.1:8080
```

The named PostgreSQL volume is preserved when containers are recreated.

### Stop without deleting data

```bash
cd /opt/note-app
sudo docker compose down
```

Never run `docker compose down -v` unless you intend to delete the production database.

### Restart

```bash
cd /opt/note-app
sudo docker compose restart
```

### Database backup

```bash
cd /opt/note-app
mkdir -p backups
sudo docker compose exec -T postgres pg_dump -U note_user note_app > "backups/note_app-$(date +%F-%H%M%S).sql"
ls -lh backups
```

Copy backups to another machine or storage provider. A backup that exists only on the VPS
is not sufficient.

### Changing production secrets

1. Back up the database.
2. Edit `/opt/note-app/.env`.
3. Changing `DEMO_PASSWORD` does **not** change an existing demo user's password, because
   `DemoDataInitializer` does not overwrite an existing account.
4. Changing `DB_PASSWORD` in `.env` alone does **not** change the password already stored
   by PostgreSQL. Update the PostgreSQL role password first, or create the production
   database fresh with the final password.

---

## Security summary

| Item | Value |
|---|---|
| Public inbound ports required by the application | none |
| Public ports that must stay closed | `8080`, `5432` |
| Cloudflare origin | `http://localhost:8080` |
| Database network | private Docker Compose network |
| Secret files kept out of Git | `.env` |

Values that must never be shared:

- `DB_PASSWORD`
- `DEMO_PASSWORD`
- Cloudflare tunnel token
- GitHub private-repository token or deploy key
