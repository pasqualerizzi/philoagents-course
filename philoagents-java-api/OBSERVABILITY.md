# Observability for PhiloAgents Java API

This document explains how to expose AI metrics, collect them via Prometheus and show them into Grafana dashboards.

## What was added

- Micrometer dependencies (`micrometer-core`, `micrometer-registry-prometheus`) in `pom.xml`.
- `application.properties` exposing the `/actuator/prometheus` endpoint.
- /monitoring folder with necessary files for prometheus and grafana configurations and dashboard templates
  - `prometheus.yml` - Prometheus configuration for metrics collection
  - `grafana/dashboards/ai-metrics-dashboard.json` - Pre-configured Grafana dashboard for AI metrics visualization
  - `grafana/provisioning/dashboards/ai-metrics-dashboard.yml` - Automatic dashboard loading configuration
  - `grafana/provisioning/datasources/prometheus.yml` - Connects Grafana to Prometheus data source automatically
- docker-compose is now involving prometheus and grafana images

![Spring Boot + Prometheus + Grafana](https://miro.medium.com/v2/resize:fit:720/format:webp/1*zg4Et9531n1MgRkeESif1w.png)


## How to verify observability is up and running

1. We suppose all services in ../docker-compose.yml are up and running. If not follow the README file in this folder first.

2. Verify the Prometheus metrics endpoint is available (default port 8000):

```bash
curl http://localhost:8000/actuator/prometheus | head -n 40
```

You should see Micrometer and application metrics related to Spring AI.

3. Configured Prometheus to scrape the `/actuator/prometheus` endpoint via prometheus.yml file in /monitoring folder.

- Confirm Prometheus is scraping opening http://localhost:9090/targets and look for philoagents-java-api target UP.

4. Create a Grafana Dashboard

    1. **Log in to Grafana**
    - Open http://localhost:3000
    - Login with username: `admin` and password: `admin`
    - You can skip changing the password for this tutorial

    2. **Prometheus Data Source**
    - This is automatically configured by the Docker setup
    - You can verify it at Configuration (gear icon) > Data Sources

    3. **Using the Pre-configured Dashboard**
    - A dashboard is already configured and automatically loaded
    - Navigate to Dashboards > General to find "Spring AI Metrics Dashboard"
    - This dashboard includes panels for token usage, request counts, and response times

    4. **Optional: Create Your Own Dashboard**
    - Click on "Dashboards" in the left sidebar
    - Click "New" > "New Dashboard"
    - Click "Add visualization"

### Next steps

- Add tracing (OpenTelemetry) for distributed traces.

