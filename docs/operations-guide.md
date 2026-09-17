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

## 4. Deployment stage

CI currently creates a tested WAR but does not change the EC2 server. Before
automatic deployment is enabled, configure a recoverable deployment method and
these GitHub repository secrets:

- `EC2_HOST`
- `EC2_USER`
- `EC2_SSH_PRIVATE_KEY`
- `EC2_HOST_KEY`

Keep application credentials in `/spring_module/.env.properties` on the server;
do not copy API keys or database passwords into the workflow.

A production deployment workflow should upload the versioned WAR, verify its
checksum, retain the previous WAR, restart the service through `systemd`, check
`/actuator/health/readiness`, and automatically roll back on failure.
