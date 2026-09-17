# DeepScan CI and operations guide

## 1. Continuous integration

`.github/workflows/ci.yml` runs for pull requests and pushes to `main` or `master`.

It performs the following steps:

1. Set up Java 17.
2. Validate the committed Gradle wrapper.
3. Run all tests.
4. Generate the JaCoCo HTML/XML coverage report.
5. Build the deployable WAR.
6. Upload reports and the WAR as GitHub Actions artifacts.

The local equivalent is:

```powershell
.\gradlew.bat clean test jacocoTestReport bootWar
```

Local reports are generated at:

- `build/reports/tests/test/index.html`
- `build/reports/jacoco/test/html/index.html`

## 2. Health checks

The default public management endpoints are deliberately limited:

- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`

Do not expose all Actuator endpoints directly to the internet. Protect them with
Spring Security, a reverse proxy allow-list, or a private monitoring network first.

After protection is configured, metrics for Prometheus can be enabled with:

```properties
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus
```

Custom verification metrics include:

- `deepscan.verification.duration`: total verification processing time, tagged by outcome.
- `deepscan.verification.completed`: completed analyses, tagged by bounded verdict values.

## 3. Request tracing

Every HTTP response contains an `X-Request-ID` header. The same value is included
in application logs as `requestId`, allowing one failed request to be followed
across controllers, services and external API calls.

A caller may provide an `X-Request-ID` containing 1-64 letters, numbers, dots,
underscores or hyphens. Unsafe values are replaced with a generated UUID.

## 4. EC2 service installation

Copy the `deploy` directory to EC2 once, then install the service:

```bash
cd /path/to/deepfake2/deploy
sudo bash install-systemd.sh ec2-user
sudo cp aws-env.properties.example /spring_module/.env.properties
sudo chown ec2-user:ec2-user /spring_module/.env.properties
sudo chmod 600 /spring_module/.env.properties
```

Fill in the real values in `/spring_module/.env.properties`. The application is
run by `deepscan.service`, and `/spring_module/current.war` points to the active
version under `/spring_module/releases`.

The installer also copies a root-owned helper to
`/usr/local/sbin/deepscan-deploy`. Give the SSH deployment user passwordless
`sudo` access to this helper only; do not grant passwordless access to arbitrary
shells. For example, edit a dedicated sudoers file with `visudo` and adapt the
user name if necessary:

```sudoers
ec2-user ALL=(root) NOPASSWD: /usr/local/sbin/deepscan-deploy *
```

The deployment script verifies the WAR checksum, switches the symlink, restarts
the service, and checks `/actuator/health/readiness`. A failed check restores the
previous symlink and restarts the previous release.

## 5. GitHub deployment configuration

Create a GitHub environment named `production`. Requiring an environment reviewer
is recommended because the deployment workflow changes the live EC2 service.

Add these environment secrets:

- `EC2_HOST`
- `EC2_USER`
- `EC2_SSH_PRIVATE_KEY`
- `EC2_HOST_KEY`
- `EC2_SSH_PORT` (optional; defaults to `22`)

Keep application credentials in `/spring_module/.env.properties` on the server;
do not copy API keys or database passwords into the workflow.

`EC2_HOST_KEY` must be a verified OpenSSH `known_hosts` line. Compare its
fingerprint with the EC2 instance fingerprint during trusted initial setup; do
not disable strict host-key checking.

The EC2 user needs key-based SSH access and the limited deployment-helper sudo
permission shown above. After configuration, open
**Actions > Deploy to EC2 > Run workflow**, choose a branch, tag, or commit, and
approve the `production` environment deployment if approval is enabled.

The workflow is manual by design. Change it to deploy automatically only after a
manual deployment and rollback have both been tested successfully.
