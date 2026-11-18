# Ubuntu VM deployment

This guide prepares an Ubuntu Linux virtual machine to run the API with Docker. It does not provision a VM and it does not deploy this workspace anywhere. Complete these steps only on a machine you control.

The default container uses the in-memory H2 database from `src/main/resources/application.yml`. Task data disappears when the container stops. That is intentional for a reproducible demo. A PostgreSQL profile is documented at the end and is not required to start the API.

## 1. VM prerequisites

- Ubuntu 22.04 LTS or 24.04 LTS.
- A user with `sudo`.
- Outbound network access so Docker can pull base images and git can clone the repository.
- Inbound SSH, usually TCP 22, limited to your own IP if the cloud firewall allows it.
- About 2 GB RAM. The JVM is capped with `MaxRAMPercentage=75.0`.

Install the packages used by the checks below:

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl git
```

## 2. Install Docker Engine

Follow the current Docker Engine instructions for Ubuntu: https://docs.docker.com/engine/install/ubuntu/

The supported path is Docker's apt repository, not the older `docker.io` snap. After installation:

```bash
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"
```

Log out and back in so the group membership applies, then check:

```bash
docker version
docker compose version
```

Both client and server sections of `docker version` must succeed. `docker compose version` confirms the Compose plugin. Current Docker Engine packages include the plugin. If `docker compose` is missing, install `docker-compose-plugin` from the same Docker apt repository.

## 3. Clone the repository

```bash
sudo mkdir -p /opt/cloud-task-api
sudo chown "$USER:$USER" /opt/cloud-task-api
git clone <YOUR_REPOSITORY_URL> /opt/cloud-task-api
cd /opt/cloud-task-api
```

Replace `<YOUR_REPOSITORY_URL>` with the GitHub URL after you create the remote yourself. This project does not create that remote.

## 4. Configure environment variables

The Compose file starts without a `.env` file. Optional settings live in `.env.example`:

```bash
cp .env.example .env
```

| Variable | Default | Effect |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | Host port published by Compose. Inside the container the application listens on 8080. |
| `SPRING_PROFILES_ACTIVE` | `default` | Leave as `default` for H2. Set `prod` only after the PostgreSQL profile is installed. |
| `JAVA_OPTS` | container memory flags | Extra JVM arguments. The entrypoint passes this value to `java`. |
| `DATABASE_URL` | unset | PostgreSQL JDBC URL. Used only by the prod profile. |
| `DATABASE_USERNAME` | unset | PostgreSQL user. Used only by the prod profile. |
| `DATABASE_PASSWORD` | unset | PostgreSQL password. Supply it in the environment. Never commit it. |

`.env` is gitignored.

## 5. Build and start

```bash
docker compose up --build -d
```

Or run the helper, which also waits for the health endpoint:

```bash
chmod +x scripts/deploy.sh
./scripts/deploy.sh
```

`deploy.sh` runs `git pull --ff-only` only when the branch has an upstream. On a fresh clone that tracking branch exists, so the script updates code before rebuilding.

Useful commands:

```bash
docker compose ps
docker compose logs -f
docker compose down
```

`docker compose down` stops and removes the container. It does not delete the image. There is no named volume in the default Compose file because H2 is in memory and there is no data to persist.

## 6. Firewall

Cloud security groups and the VM firewall are separate. Open only what you need.

On the VM, allow SSH and the published application port, then enable UFW:

```bash
sudo ufw allow OpenSSH
sudo ufw allow 8080/tcp
sudo ufw enable
sudo ufw status
```

If you change `SERVER_PORT`, allow that host port instead of 8080.

In the cloud firewall, allow TCP 22 from your IP and TCP 8080 from the clients that should call the API. Do not open Docker's internal ports. The container network `task-api-net` is a bridge and is not published except for the mapped host port.

This demo has no authentication. Treat an exposed port as a public demo endpoint, or restrict the source addresses.

## 7. Verify health

On the VM:

```bash
curl -fsS http://127.0.0.1:8080/actuator/health
curl -fsS http://127.0.0.1:8080/api/info
```

A healthy process returns HTTP 200 and `{"status":"UP"}`. From another machine, use the VM's public address and the published port. Swagger UI is at:

```text
http://<vm-host>:8080/swagger-ui/index.html
```

`docker compose ps` shows the container health as `healthy` after the start period. The check calls `curl` against `http://localhost:8080/actuator/health` inside the container.

## 8. Update after new commits

On the VM:

```bash
cd /opt/cloud-task-api
git pull --ff-only
docker compose up --build -d
```

`./scripts/deploy.sh` does the same thing when an upstream is configured. The manual GitHub Actions workflow in `.github/workflows/deploy.yml` runs those commands over SSH once `VM_HOST`, `VM_USER`, `VM_SSH_KEY`, and `VM_APP_DIR` are stored as GitHub Actions secrets.

## 9. Logs and restart

```bash
docker compose logs -f
docker compose restart
docker compose up -d
```

`restart` restarts the current container. `up -d` recreates it when the Compose file changed. `logs -f` follows application and container output. Leave it with Ctrl+C; that does not stop the API.

## 10. Troubleshooting

| Symptom | What to check |
| --- | --- |
| `docker: permission denied` | The user is not in the `docker` group, or the shell was not restarted after `usermod`. |
| `Cannot connect to the Docker daemon` | `sudo systemctl status docker` and start the service if it is stopped. |
| Host port already allocated | Another process owns 8080. Change `SERVER_PORT` or stop that process. |
| Container stays `unhealthy` | `docker compose logs --tail=200`. Confirm `/actuator/health` from inside: `docker compose exec api curl -fsS http://localhost:8080/actuator/health`. |
| `git pull` rejected | The VM has local edits. Keep the clone clean and deploy only committed revisions. |
| Blank page on the public IP | The cloud firewall or `ufw` is still blocking the published port. |
| Application fails only with `SPRING_PROFILES_ACTIVE=prod` | PostgreSQL is not reachable, or `DATABASE_PASSWORD` is unset. Switch back to `default` to use H2. |
| Old code still running | `docker compose up --build -d` rebuilds the image. `restart` alone does not pick up source changes. |

## PostgreSQL profile

The default image does not need PostgreSQL. To use it later:

1. Provide a PostgreSQL 16+ database that the VM or another container can reach.
2. Copy `application-prod.yml.example` to `src/main/resources/application-prod.yml` or `./config/application-prod.yml` on the server. Do not commit the copy.
3. Export `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`.
4. Set `SPRING_PROFILES_ACTIVE=prod`.
5. Rebuild with `docker compose up --build -d`.

The example uses `ddl-auto: update` so a new database can boot without Flyway. Move to migrations and `ddl-auto: validate` before storing data you need to keep. The PostgreSQL JDBC driver is already in the Maven build. No database password is stored in the repository.

If you add a database container later, attach it to the `task-api-net` network and add a named volume for the database files. The application container still does not need a volume while it uses H2.
