# Backend GitHub Actions

`.github/workflows/backend-ci.yml` replaces the GitLab test, build, and manual
deployment stages. Pushes and pull requests run Java 21 Maven tests followed by a
Docker build on GitHub-hosted Ubuntu runners. Test reports are retained for seven
days. These jobs consume the private repository owner's GitHub Actions allowance.

As in the GitLab pipeline, the build job validates the Docker image without
publishing it. Deployment rebuilds from the same workflow commit using the
existing production Compose file and project name `nextgoal`.

## Configure the Ubuntu deployment runner

1. In the GitHub repository, open Settings > Actions > Runners > New self-hosted
   runner. Select Linux and x64 and follow the displayed installation and
   registration commands on the Ubuntu deployment server. This is a separate
   installation from GitLab Runner; use its own directory and keep its token private.
2. Add the custom label `ubuntu-deploy` during registration or in the runner's
   GitHub settings. Keep the default `self-hosted`, `linux`, and `x64` labels.
3. Install and start the runner service using the generated runner directory's
   `sudo ./svc.sh install` and `sudo ./svc.sh start` commands after registration.
4. Ensure the service account can use Docker and Docker Compose v2, and can read
   `/etc/default/nextgoal-env`. The existing GitLab service account's permissions
   do not automatically apply to the new GitHub runner account. Docker access is
   privileged; use a dedicated trusted deployment account.
5. Where supported by the repository plan, configure the `production/ubuntu`
   environment to permit only the default branch and require deployment approval.
   Protect the default branch and review workflow changes before merging them.

The existing server environment file remains the source of production secrets.
Do not commit it or copy its contents into workflow logs. This deployment method
does not require SSH secrets in GitHub.

## Run CI and deploy

1. Commit and push the workflow and this guide. Keep unrelated local changes out
   of the migration commit. Merge the workflow into the GitHub default branch;
   the manual Run workflow button requires the workflow to exist there.
2. Ensure the default branch contains the backend version you intend to deploy.
   Uploading feature branches does not merge their code into the default branch.
3. Open Actions > Backend CI > Run workflow. Select the default branch and check
   the deploy input. Tests and the Docker build must pass before deployment runs.
4. Check the deployment logs and verify the application's health afterward.
   Compose `ps` reports container status; it is not an application health test.

Pushes, pull requests, and manual runs without the deploy input never deploy.
Manual runs on other branches also skip deployment. Production deployments are
serialized and an active deployment is not canceled by a newer run.

The GitLab pipeline file is retained for migration fallback. Stop using its manual
deployment once GitHub takes over; the two platforms do not share deployment locks.

References:
- https://docs.github.com/en/actions/how-tos/manage-runners/self-hosted-runners/add-runners
- https://docs.github.com/en/actions/how-tos/manage-runners/self-hosted-runners/configure-the-application
