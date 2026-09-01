# Legacy Ubuntu production deployment

This project currently uses the original GitLab CI deployment workflow. The
test and Docker build jobs run first; the protected Ubuntu shell runner then
builds the backend again from its GitLab checkout with Docker Compose.

## 1. Server environment

Create `/etc/default/nextgoal-env` and keep it outside Git:

```sh
sudo nano /etc/default/nextgoal-env
sudo chown root:gitlab-runner /etc/default/nextgoal-env
sudo chmod 0640 /etc/default/nextgoal-env
```

Required values:

```ini
POSTGRES_DB=nextgoal
DB_USERNAME=replace-with-a-new-database-user
DB_PASSWORD=replace-with-a-new-random-password

SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/nextgoal
SPRING_DATASOURCE_USERNAME=replace-with-the-same-database-user
SPRING_DATASOURCE_PASSWORD=replace-with-the-same-random-password

SPRING_PROFILES_ACTIVE=prod
JWT_SECRET=replace-with-at-least-32-random-bytes
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000
```

`DB_USERNAME` must equal `SPRING_DATASOURCE_USERNAME`, and `DB_PASSWORD` must
equal `SPRING_DATASOURCE_PASSWORD`. Do not reuse credentials that appeared in
Git history or screenshots.

The legacy Compose file does not require `POSTGRES_IMAGE`,
`POSTGRES_VOLUME_NAME`, or `BACKEND_IMAGE`.

## 2. Runner access

The runner must use the `shell` executor and have the `ubuntu-deploy` tag:

```sh
sudo usermod --append --groups docker gitlab-runner
sudo systemctl restart gitlab-runner
sudo -u gitlab-runner docker version
sudo -u gitlab-runner docker compose version
sudo -u gitlab-runner test -r /etc/default/nextgoal-env
```

Protect the runner and default branch. Docker-group access is root-equivalent.

## 3. Validate Compose

From a clean checkout containing the `genz` directory:

```sh
cd genz
sudo -u gitlab-runner docker compose \
  --project-name nextgoal \
  --env-file /etc/default/nextgoal-env \
  -f docker-compose.prod.yaml \
  config --quiet
```

The Compose project creates a volume named
`nextgoal_nextgoal-db-data` during the first deployment.

## 4. Deploy

Merge the release into the GitLab default branch, wait for `test` and
`build_image`, then run the manual `deploy_ubuntu` job. The job runs:

```sh
docker compose \
  --project-name nextgoal \
  --env-file /etc/default/nextgoal-env \
  -f docker-compose.prod.yaml \
  up -d --build backend
```

Verify:

```sh
sudo docker ps --filter name=nextgoal
sudo docker logs --tail 100 nextgoal-backend
curl --fail http://127.0.0.1:8080/actuator/health/readiness
```

## 5. Known limitations of the restored workflow

- The image built in `build_image` is not transferred to the deployment runner.
- The Ubuntu runner rebuilds the image from source during deployment.
- PostgreSQL uses the mutable `postgres:17` tag.
- The job does not create a database backup or automatically roll back.
- Port 8080 is directly published unless the host firewall blocks it.

Back up PostgreSQL before important deployments and configure Nginx/TLS before
public exposure. OTP still needs a real SMS provider, wallet deposit remains a
virtual ledger operation, and distributed authentication rate limiting and a
private vulnerability scan remain outstanding.
