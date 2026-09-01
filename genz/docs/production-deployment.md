# Ubuntu production deployment

This project deploys through GitLab CI to a dedicated Ubuntu shell runner. CI
tests the source, builds one commit-addressed backend image, pushes it to the
GitLab Container Registry, and then asks the Ubuntu runner to pull that exact
image. The production server must not be populated by copying source trees over
older source trees.

## 1. Install or verify Ubuntu prerequisites

The server needs Docker Engine with the Compose plugin, GitLab Runner, Nginx,
`curl`, and Certbot. If Docker is already installed, verify it instead of
reinstalling it:

```sh
sudo docker version
sudo docker compose version
sudo apt update
sudo apt install -y ca-certificates curl nginx snapd
```

For a new Docker installation, use Docker's official Ubuntu repository:
<https://docs.docker.com/engine/install/ubuntu/>.

Install GitLab Runner from its official repository. Inspect the downloaded
repository script before running it:

```sh
cd /tmp
curl -L "https://packages.gitlab.com/install/repositories/runner/gitlab-runner/script.deb.sh" -o gitlab-runner-repo.sh
less gitlab-runner-repo.sh
sudo bash gitlab-runner-repo.sh
sudo apt install -y gitlab-runner
sudo systemctl enable --now gitlab-runner
```

Install Certbot using its supported snap package:

```sh
sudo snap install --classic certbot
sudo ln -s /snap/bin/certbot /usr/local/bin/certbot
```

If `/usr/local/bin/certbot` already exists, do not create the link again.

## 2. Release gates

Before deploying:

1. Rotate the PostgreSQL password and JWT secret that previously appeared in
   Git history or screenshots. Old values must not be reused.
2. Confirm `/api/wallet/deposit` is intentionally a virtual ledger operation;
   it does not verify settlement with a bank or payment provider.
3. Keep OTP console delivery disabled. Production OTP deliberately fails closed
   until a real SMS sender is integrated.
4. Protect the GitLab default branch, production environment, and the dedicated
   `ubuntu-deploy` runner.
5. Confirm DNS points the production API hostname to this Ubuntu server and only
   ports 22, 80, and 443 are publicly reachable.

## 3. Create the PostgreSQL volume

This is a new deployment, so create a stable external volume before the first
pipeline deployment:

```sh
sudo docker volume create nextgoal-db-data
sudo docker volume inspect nextgoal-db-data
```

Set `POSTGRES_VOLUME_NAME=nextgoal-db-data`. Compose treats this as an external
volume and refuses to deploy if it is missing, rather than silently creating a
different empty database because the Compose project name changed.

## 4. Server environment file

Copy `.env.example` to `/etc/default/nextgoal-env`. The file must be readable by
the GitLab runner but not by other users:

```sh
sudo groupadd --force nextgoal
sudo usermod --append --groups nextgoal gitlab-runner
sudo touch /etc/default/nextgoal-env
sudo chown root:nextgoal /etc/default/nextgoal-env
sudo chmod 0640 /etc/default/nextgoal-env
sudo nano /etc/default/nextgoal-env
```

Required values include:

```ini
POSTGRES_IMAGE=postgres:17@sha256:replace-with-a-real-digest
POSTGRES_VOLUME_NAME=nextgoal-db-data
POSTGRES_DB=nextgoal
SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/nextgoal
SPRING_DATASOURCE_USERNAME=nextgoal_app
SPRING_DATASOURCE_PASSWORD=replace-with-a-new-random-password

SPRING_PROFILES_ACTIVE=prod
JWT_SECRET=replace-with-at-least-32-random-bytes
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000

CORS_ALLOWED_ORIGIN_PATTERNS=https://app.example.com
WEBSOCKET_ALLOWED_ORIGIN_PATTERNS=https://app.example.com
```

For a native Android/iOS Flutter application, browsers do not enforce CORS on
the mobile HTTP client. Use the exact HTTPS origin of any Flutter Web or admin
frontend. Do not use `*` with authenticated endpoints.

Resolve and pin the PostgreSQL image once instead of deploying a mutable major
tag:

```sh
sudo docker pull postgres:17
sudo docker image inspect postgres:17 --format '{{index .RepoDigests 0}}'
```

Copy the resulting `postgres@sha256:...` value into `POSTGRES_IMAGE`.

Generate new secrets locally on the server:

```sh
openssl rand -base64 48
openssl rand -hex 64
```

Use the first value as the database password and the second as `JWT_SECRET`.
Because this is a new database, PostgreSQL creates the configured role and
password on its first start. Do not change `SPRING_DATASOURCE_USERNAME` after
the volume has been initialized without deliberately creating or renaming the
database role. Rotating `JWT_SECRET` later logs out all authenticated users.

## 5. Runner directories and permissions

The runner needs Docker access, read access to the environment file, and write
access only to the application backup directory:

```sh
sudo usermod --append --groups docker,nextgoal gitlab-runner
sudo install -d -o gitlab-runner -g nextgoal -m 0750 /var/backups/nextgoal
sudo systemctl restart gitlab-runner
sudo -u gitlab-runner docker version
sudo -u gitlab-runner docker compose version
sudo -u gitlab-runner test -r /etc/default/nextgoal-env
sudo -u gitlab-runner test -w /var/backups/nextgoal
```

Membership in the Docker group is root-equivalent. Use a dedicated protected
runner and do not let untrusted branches or fork pipelines use its
`ubuntu-deploy` tag.

## 6. GitLab project settings

1. Enable the project Container Registry.
2. Create a project runner in **Settings > CI/CD > Runners**.
3. Give it the tag `ubuntu-deploy`, disable untagged jobs, lock it to this
   project, and mark it protected.
4. Register it on Ubuntu with the **shell** executor:

   ```sh
   sudo gitlab-runner register
   ```

5. Protect the default branch and the `production/ubuntu` environment.
6. Restrict who can run the manual production deployment job.

The build job may run on a separate Docker-capable runner. Only the deploy job
must use the protected Ubuntu shell runner.

## 7. Validate before the first deployment

The GitLab runner checks out committed code. Local uncommitted changes and files
copied manually to `~/next` are not deployable. Commit and push the intended
release, then confirm the pipeline test and image build jobs pass.

On Ubuntu, verify the environment without printing its secrets:

```sh
sudo -u gitlab-runner bash -lc '
  export BACKEND_IMAGE=registry.gitlab.com/example/project/backend:test
  cd /path/to/a-clean-runner-checkout/genz
  docker compose --project-name nextgoal \
    --env-file /etc/default/nextgoal-env \
    -f docker-compose.prod.yaml config --quiet
'
```

The pipeline uses `GIT_STRATEGY: clone`, so stale untracked Java packages cannot
leak into the Docker build context. A clean build compiles 343 main source files;
the broken overlaid server tree compiled 445.

## 8. Deploy

1. Push or merge the release into the protected default branch.
2. Open the GitLab pipeline and wait for `test` and `build_image` to pass.
3. Run the manual `deploy_ubuntu` job.
4. The job validates Compose, creates a PostgreSQL custom-format backup when the
   database is already running, pulls the commit-SHA image, starts it without a
   server-side build, and waits up to two minutes for readiness.
5. If readiness fails, the job prints backend logs, restores the prior backend
   image, and fails. Database migrations are not automatically reversed.

Verify afterward:

```sh
curl --fail http://127.0.0.1:8080/actuator/health/liveness
curl --fail http://127.0.0.1:8080/actuator/health/readiness
sudo docker ps --filter name=nextgoal
sudo docker logs --tail 100 nextgoal-backend
ls -lh /var/backups/nextgoal
```

## 9. HTTPS reverse proxy

Spring Boot binds only to `127.0.0.1:8080`; expose it through Nginx. Replace
`api.example.com` with the real API domain. Create
`/etc/nginx/sites-available/nextgoal` with this configuration:

```nginx
server {
    listen 80;
    server_name api.example.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

Enable the Nginx site, verify its configuration, obtain a certificate, and test
renewal:

```sh
sudo ln -s /etc/nginx/sites-available/nextgoal /etc/nginx/sites-enabled/nextgoal
sudo nginx -t
sudo systemctl reload nginx
sudo certbot --nginx -d api.example.com
sudo certbot renew --dry-run
```

After HTTPS works, verify
`https://api.example.com/actuator/health/readiness`. Do not publicly expose port
8080 or PostgreSQL port 5432.

## 10. Backup restore drill

Backups are useful only after a restore has been tested. Restore a selected dump
to a separate PostgreSQL instance or maintenance database and validate the
Flyway history and important row counts. Do not overwrite the production
database during a drill.

The automatic deploy backup is stored in `/var/backups/nextgoal`. Copy backups
off the server, encrypt them, define retention, and monitor backup failures and
disk usage. A single Docker volume is not high availability; use managed
PostgreSQL or a tested replication/failover design when production availability
requires it.
