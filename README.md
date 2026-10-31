# 202512117 — WSSOA Lab 8 Updated
## Kubernetes, CI/CD & Monitoring
### Web Services & SOA Laboratory | Semester 3

> **Student ID:** 202512117  
> **Lab:** Lab 8 — Kubernetes, GitHub Actions CI/CD, Prometheus & Grafana  
> **Built on:** Lab 7 API Gateway microservices architecture

---

## Table of Contents
1. [Application Overview](#1-application-overview)
2. [What Changed (Lab 7 vs Lab 8)](#2-what-changed)
3. [Architecture](#3-architecture)
4. [Technologies Used](#4-technologies-used)
5. [Prerequisites](#5-prerequisites)
6. [Kubernetes Environment](#6-kubernetes-environment)
7. [Applying Manifests & Verifying Resources](#7-applying-manifests--verifying-resources)
8. [Accessing & Testing the Gateway](#8-accessing--testing-the-gateway)
9. [Scaling & Self-Healing](#9-scaling--self-healing)
10. [GitHub Actions CI/CD](#10-github-actions-cicd)
11. [Prometheus Monitoring](#11-prometheus-monitoring)
12. [Grafana Dashboard](#12-grafana-dashboard)
13. [Local Docker Compose (Dev)](#13-local-docker-compose-dev)
14. [Configuration — No Secrets Committed](#14-configuration--no-secrets-committed)
15. [Troubleshooting](#15-troubleshooting)
16. [Screenshot Manifest](#16-screenshot-manifest)
17. [Lab 7 Discussion Questions](#17-lab-7-discussion-questions)
18. [Lab 7 Reflection](#18-lab-7-reflection)
19. [Submission Checklist](#19-submission-checklist)

---

## 1. Application Overview

This project extends the Lab 7 microservices application by deploying it on **Kubernetes** with a **GitHub Actions CI/CD pipeline** and **Prometheus + Grafana monitoring**.

The application consists of 4 Spring Boot microservices:

| Service | Port | Responsibility |
|---|---|---|
| **API Gateway** | 3000 | Single client-facing entry point. Routes `/users`, `/products`, `/orders`. |
| **User Service** | 3001 | Manages user data via MongoDB Atlas. |
| **Product Service** | 3002 | Manages product data via MongoDB Atlas. |
| **Order Service** | 3003 | Manages orders; calls User & Product services internally. |

**Database:** MongoDB Atlas (external, cloud-managed). No database runs in Kubernetes.

**Application flow:**
```
Client / Postman  →  API Gateway (NodePort 30000)
                  →  User / Product / Order Services (ClusterIP)
                  →  MongoDB Atlas (external)
```

---

## 2. What Changed

### Lab 7 → Lab 8 Changes

| Area | Lab 7 | Lab 8 |
|---|---|---|
| Deployment | Docker Compose only | **Kubernetes (Docker Desktop)** |
| Database | MongoDB Atlas (same) | MongoDB Atlas (same) — no change |
| Service discovery | Docker DNS (service names) | **Kubernetes DNS** (ClusterIP services) |
| Configuration | `.env` / Docker environment | **ConfigMap** (non-sensitive) + **Secret** (Atlas URIs) |
| CI/CD | None | **GitHub Actions**: Maven build → test → Docker image → push |
| Image versioning | `latest` tag only | **Git SHA tag** (traceable per commit) |
| Monitoring | Prometheus only (basic) | **Prometheus + Grafana dashboard** |
| Health probes | Docker healthcheck | **Kubernetes liveness + readiness probes** |
| Scaling | Manual Docker restart | `kubectl scale --replicas=N` |
| Self-healing | Not demonstrated | **Kubernetes auto-restarts deleted pods** |
| Cloud deployment | Local only | Cloud platform (see Part C section) |

---

## 3. Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     INTERNET / CLIENT                        │
│                  Postman / Browser                           │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP (port 30000)
┌──────────────────────────▼──────────────────────────────────┐
│                 KUBERNETES CLUSTER (lab8 namespace)          │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │   API Gateway (NodePort :30000)                        │  │
│  │   Routes: /users → user-service:3001                  │  │
│  │           /products → product-service:3002            │  │
│  │           /orders → order-service:3003                │  │
│  └────────┬────────────────┬──────────────┬──────────────┘  │
│           │                │              │                  │
│  ┌────────▼──────┐ ┌───────▼──────┐ ┌────▼──────────────┐  │
│  │ user-service  │ │product-service│ │  order-service    │  │
│  │ ClusterIP     │ │ ClusterIP    │ │  ClusterIP        │  │
│  │ :3001         │ │ :3002        │ │  :3003            │  │
│  └───────────────┘ └──────────────┘ └───────────────────┘  │
│                                                              │
│  ConfigMap: lab8-config (service URLs)                       │
│  Secret: lab8-mongodb-secret (Atlas URIs)                    │
└──────────────────────────┬──────────────────────────────────┘
                           │ mongodb+srv://
┌──────────────────────────▼──────────────────────────────────┐
│              MongoDB Atlas (External Cloud DB)               │
│   cluster0.eg8blnj.mongodb.net                               │
│   userdb | productdb | orderdb                               │
└─────────────────────────────────────────────────────────────┘

Monitoring (local Docker):
Prometheus (:9090) → scrapes /actuator/prometheus from all services
Grafana (:3005) → connects to Prometheus → Lab 8 dashboard
```

---

## 4. Technologies Used

| Technology | Version | Purpose |
|---|---|---|
| Spring Boot | 3.x | All 4 microservices |
| Java | 17 | JDK for Spring Boot |
| Maven | 3.x | Build and dependency management |
| Docker | Latest | Container runtime |
| Kubernetes | 1.30+ | Container orchestration |
| kubectl | Latest | Kubernetes CLI |
| Docker Desktop | Latest | Local Kubernetes cluster |
| MongoDB Atlas | Cloud | External managed database |
| GitHub Actions | N/A | CI/CD pipeline |
| Docker Hub | N/A | Container image registry |
| Prometheus | Latest | Metrics collection |
| Grafana | Latest | Metrics visualization |

---

## 5. Prerequisites

- Docker Desktop installed and **running**
- Kubernetes enabled in Docker Desktop (Settings → Kubernetes → Enable)
- `kubectl` installed and configured
- MongoDB Atlas account with `cluster0.eg8blnj.mongodb.net` active
- GitHub account (for CI/CD)
- Docker Hub account (for image registry)
- Postman (for API testing)

---

## 6. Kubernetes Environment

### Check context and cluster

```bash
# Verify active context
kubectl config current-context
# Expected: docker-desktop

# Confirm node is ready
kubectl get nodes
# Expected: docker-desktop   Ready   ...
```

### Create the lab8 namespace

```bash
kubectl apply -f k8s/namespace.yaml
kubectl get namespaces
```

---

## 7. Applying Manifests & Verifying Resources

### Step-by-step apply order

```bash
# 1. Namespace (must be first)
kubectl apply -f k8s/namespace.yaml

# 2. Create Secret with real Atlas credentials (never committed to Git)
kubectl create secret generic lab8-mongodb-secret \
  --from-literal=MONGO_URI_USER="mongodb+srv://202512117_db_user:YOURPWD@cluster0.eg8blnj.mongodb.net/userdb?retryWrites=true&w=majority" \
  --from-literal=MONGO_URI_PRODUCT="mongodb+srv://202512117_db_user:YOURPWD@cluster0.eg8blnj.mongodb.net/productdb?retryWrites=true&w=majority" \
  --from-literal=MONGO_URI_ORDER="mongodb+srv://202512117_db_user:YOURPWD@cluster0.eg8blnj.mongodb.net/orderdb?retryWrites=true&w=majority" \
  -n lab8

# 3. ConfigMap
kubectl apply -f k8s/configmap.yaml

# 4. All deployments and services at once
kubectl apply -f k8s/ -n lab8

# 5. Verify
kubectl get deployments -n lab8
kubectl get pods -n lab8
kubectl get services -n lab8
```

### Expected pod states

```
NAME                               READY   STATUS    RESTARTS   AGE
api-gateway-xxxxx-xxxxx            1/1     Running   0          2m
user-service-xxxxx-xxxxx           1/1     Running   0          2m
product-service-xxxxx-xxxxx        1/1     Running   0          2m
order-service-xxxxx-xxxxx          1/1     Running   0          2m
```

### Probe configuration (example — user-service)

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health    # Spring Boot Actuator health endpoint
    port: 3001
  initialDelaySeconds: 45     # Allow time for JVM startup
  periodSeconds: 20
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health
    port: 3001
  initialDelaySeconds: 30
  periodSeconds: 10
  failureThreshold: 3
```

**Liveness** — Kubernetes restarts the container if this fails  
**Readiness** — Pod only receives traffic when this passes (critical during startup and rolling updates)

---

## 8. Accessing & Testing the Gateway

The API Gateway is exposed as **NodePort 30000** (local Kubernetes).

```
Gateway URL: http://localhost:30000
```

### Endpoints via gateway

| Endpoint | Method | Description |
|---|---|---|
| `/health` | GET | Gateway health check |
| `/users` | GET | List all users |
| `/users/{id}` | GET | Get user by ID |
| `/users` | POST | Create user |
| `/products` | GET | List all products |
| `/products/{id}` | GET | Get product by ID |
| `/products` | POST | Create product |
| `/orders` | GET | List all orders |
| `/orders` | POST | Create order |

### Internal routing (Kubernetes Service DNS)

```
api-gateway → http://user-service:3001/users
api-gateway → http://product-service:3002/products
api-gateway → http://order-service:3003/orders
```

Service names resolve via Kubernetes DNS within the `lab8` namespace.

---

## 9. Scaling & Self-Healing

### Scale user-service to 3 replicas

```bash
kubectl scale deployment user-service --replicas=3 -n lab8
kubectl get pods -n lab8
# Expected: 3 user-service pods Running
```

### Demonstrate self-healing

```bash
# List pods to find a user-service pod name
kubectl get pods -n lab8

# Delete one pod (Kubernetes will immediately create a replacement)
kubectl delete pod <user-service-pod-name> -n lab8

# Watch Kubernetes create replacement
kubectl get pods -n lab8
# The deleted pod shows Terminating; a new pod appears Running
```

**Why this works:** The Deployment maintains `replicas: N` as the desired state. When a pod is deleted, the Deployment controller detects the deficit and creates a new pod automatically.

---

## 10. GitHub Actions CI/CD

### Workflow location
`.github/workflows/ci.yml`

### Flow
```
Git Push → Build/Test (Maven) → Docker Build → Push to Docker Hub (SHA tag) → Verify
```

### Required GitHub Secrets (Settings → Secrets → Actions)

| Secret Name | Description |
|---|---|
| `DOCKER_USERNAME` | Your Docker Hub username |
| `DOCKER_PASSWORD` | Your Docker Hub password or PAT |

> MongoDB Atlas URIs are NOT passed to GitHub Actions — they are applied directly to the Kubernetes Secret locally and never committed.

### Triggering the workflow

```bash
git add .
git commit -m "feat: lab8 k8s deployment"
git push origin main
```

Open: `https://github.com/202512117/202512117_Lab8_Updated/actions`

### Image naming convention

```
docker.io/<username>/202512117-lab8-api-gateway:<github.sha>
docker.io/<username>/202512117-lab8-user-service:<github.sha>
docker.io/<username>/202512117-lab8-product-service:<github.sha>
docker.io/<username>/202512117-lab8-order-service:<github.sha>
```

Using the **Git commit SHA** as the image tag ensures every image is uniquely traceable to the exact code that produced it.

---

## 11. Prometheus Monitoring

### Start Prometheus (Docker)

```bash
docker run -d \
  --name prometheus \
  -p 9090:9090 \
  --add-host=host.docker.internal:host-gateway \
  -v $(pwd)/monitoring/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus
```

### Access Prometheus
URL: `http://localhost:9090`

### Verify targets
Navigate to: **Status → Targets**  
All 4 services should show **UP** state.

### Key PromQL queries

```promql
# Are all services up?
up

# HTTP request rate (last 5 minutes)
rate(http_server_requests_seconds_count[5m])

# Error rate (5xx responses)
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# Average request latency
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])

# JVM memory usage
jvm_memory_used_bytes{area="heap"}
```

---

## 12. Grafana Dashboard

### Start Grafana (Docker)

```bash
docker run -d \
  --name grafana \
  -p 3005:3000 \
  -e GF_SECURITY_ADMIN_PASSWORD=admin \
  grafana/grafana
```

### Access Grafana
URL: `http://localhost:3005`  
Credentials: `admin` / `admin`

### Setup

1. **Add Prometheus data source:**  
   Configuration → Data Sources → Add → Prometheus → URL: `http://host.docker.internal:9090` → Save & Test

2. **Create Lab 8 Dashboard:**  
   Dashboards → New → Add panel for each monitoring question:

| Panel Title | PromQL | Answers |
|---|---|---|
| Service Availability | `up` | Are targets reachable? |
| Request Rate | `rate(http_server_requests_seconds_count[5m])` | How much traffic? |
| Error Rate | `rate(http_server_requests_seconds_count{status=~"5.."}[5m])` | Are failures increasing? |
| Avg Latency | `rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])` | How long are requests taking? |

### Generate traffic to observe dashboard changes

```bash
# Send several requests through the gateway
curl http://localhost:30000/users
curl http://localhost:30000/products
curl http://localhost:30000/orders
# Repeat 10-20 times, then refresh Grafana
```

---

## 13. Local Docker Compose (Dev)

For local development and screenshot capture:

```bash
# Create .env file with real Atlas credentials (see .env.example)
cp .env.example .env
# Edit .env and set real passwords

# Start all services + monitoring
docker-compose up --build

# Wait ~60 seconds for JVM startup
# Gateway:    http://localhost:3000
# Prometheus: http://localhost:9090
# Grafana:    http://localhost:3005

# Stop
docker-compose down
```

---

## 14. Configuration — No Secrets Committed

| Config Type | Where | Example |
|---|---|---|
| Non-sensitive URLs | `k8s/configmap.yaml` | `USER_SERVICE_URL=http://user-service:3001` |
| MongoDB Atlas URIs | `kubectl create secret` (never in file) | Applied at deploy time only |
| Docker Hub credentials | GitHub Secrets | `DOCKER_USERNAME`, `DOCKER_PASSWORD` |
| Local dev credentials | `.env` (gitignored) | Not committed — see `.env.example` |

**Security rules followed:**
- ✅ `.env` is in `.gitignore`
- ✅ `mongodb-secret.yaml` has placeholder values only
- ✅ No Atlas URI, username, or password in any committed file
- ✅ GitHub Secrets used for Docker Hub credentials in CI

---

## 15. Troubleshooting

### Pod not starting

```bash
# Check events and status
kubectl describe pod <pod-name> -n lab8

# Check application logs
kubectl logs <pod-name> -n lab8

# Check if Secret/ConfigMap is correctly applied
kubectl get secret lab8-mongodb-secret -n lab8
kubectl get configmap lab8-config -n lab8
```

### Service unreachable

```bash
# Check endpoints are populated (means pods are Ready)
kubectl get endpoints -n lab8

# Check selectors match
kubectl describe service user-service -n lab8
```

### Gateway returns 502/503

- Internal service is not ready yet — wait for readiness probe to pass
- Check service pod logs: `kubectl logs <service-pod> -n lab8`

### Prometheus targets DOWN

- Ensure services are reachable on expected ports
- Check `host.docker.internal` resolves on your OS
- Windows: add `host.docker.internal` to `C:\Windows\System32\drivers\etc\hosts`

### Docker Desktop Kubernetes won't start

- Ensure Docker Desktop has at least 2 CPU and 4 GB RAM assigned (Settings → Resources)
- Restart Docker Desktop
- Check: `kubectl cluster-info`

---

## 16. Screenshot Manifest

All screenshots are in `Screenshots/` folder.

| Screenshot | Requirement Demonstrated | Lab |
|---|---|---|
| `SS01-Lab7-GatewayPostman.png` | Lab 7 baseline: successful gateway request/response | Lab 7 |
| `SS02-K8s-ContextAndNodes.png` | Kubernetes context + node Ready status | Lab 8 |
| `SS03-K8s-Manifests.png` | K8s YAML files — gateway + service + configmap | Lab 8 |
| `SS04-K8s-DeploymentsPodsServices.png` | All deployments/pods/services running in lab8 | Lab 8 |
| `SS05-K8s-GatewayPostman.png` | 2 Postman requests through Kubernetes gateway | Lab 8 |
| `SS06-K8s-Scaling.png` | user-service scaled to 3 replicas | Lab 8 |
| `SS07-K8s-SelfHealing.png` | Pod deleted → replacement created automatically | Lab 8 |
| `SS08-K8s-Troubleshoot.png` | kubectl describe + logs + endpoints output | Lab 8 |
| `SS09-CICD-GithubActionsRun.png` | Successful GitHub Actions CI run (all steps green) | Lab 8 |
| `SS10-Prometheus-TargetsAndQuery.png` | Prometheus Targets UP + PromQL query result | Lab 8 |
| `SS11-Grafana-Dashboard.png` | Grafana Lab 8 dashboard with panels | Lab 8 |
| `SS12-Monitoring-AfterTraffic.png` | Dashboard reflecting API traffic changes | Lab 8 |
| `SS13-Architecture.png` | Final architecture diagram | Lab 8 |

---

## 17. Lab 7 Discussion Questions

### Q: Why introduce an API Gateway instead of letting clients call each service directly?

An API Gateway provides a **single entry point** for all client requests, hiding the internal structure of the microservices system. Without it, clients would need to know the address and port of every individual service — creating tight coupling between the client and the internal architecture. When services are added, moved, or scaled, all clients would need to be updated. The gateway centralizes cross-cutting concerns such as **request logging**, **error handling**, **rate limiting**, and **authentication** in one place instead of duplicating them in every service. It also enforces network isolation: internal services are not exposed externally, reducing the attack surface.

### Q: Static config-based service discovery vs dynamic discovery

| Aspect | Static / Config-based (this lab) | Dynamic (Consul, Eureka, Kubernetes DNS) |
|---|---|---|
| How it works | Service URLs stored in env vars / ConfigMap | Services register themselves; registry updated automatically |
| Change required when service moves | Update env var / ConfigMap and redeploy | None — discovery finds new location automatically |
| Suitable for | Fixed topology, small systems | Auto-scaling, rolling deployments, large fleets |
| Example | `USER_SERVICE_URL=http://user-service:3001` | Kubernetes DNS: `user-service.lab8.svc.cluster.local` |

In Kubernetes, DNS-based service discovery is already dynamic — Service names resolve to the current pod IPs behind the scenes, so Kubernetes provides the benefits of dynamic discovery without a separate registry.

---

## 18. Lab 7 Reflection

**What the API Gateway and cloud deployment changed about how the system is used and operated, compared to Lab 6:**

In Lab 6, all services were accessible directly on separate ports, requiring clients to know the address of each individual service. Adding the API Gateway in Lab 7 created a single, stable entry point that hides the internal topology entirely — the client only needs one URL. This made the system far easier to operate: logging, error handling, and routing rules are managed in one place instead of scattered across services. When we moved from local Docker Compose to cloud deployment, the service URLs became configuration values rather than `localhost` references, demonstrating the key principle that the same application image runs anywhere just by changing environment variables. The public URL also means the system is genuinely production-like — reachable from any device over the internet, not just the developer's machine.

---

## 19. Submission Checklist

| Item | Status |
|---|---|
| Lab 7 application verified (Docker Compose baseline) | ☐ |
| Kubernetes context/node verified | ☐ |
| `lab8` namespace created | ☐ |
| Secret applied (Atlas URIs — not committed) | ☐ |
| ConfigMap applied | ☐ |
| All 4 Deployments created | ☐ |
| All 4 Services created | ☐ |
| Health & readiness probes configured | ☐ |
| Pods become Ready | ☐ |
| Gateway tested via Postman (Kubernetes) | ☐ |
| User Service scaled to 3 replicas | ☐ |
| Self-healing demonstrated | ☐ |
| `kubectl describe` + logs + endpoints captured | ☐ |
| GitHub Actions CI/CD workflow passed | ☐ |
| Versioned Docker image pushed to Docker Hub | ☐ |
| Prometheus targets UP | ☐ |
| PromQL query executed | ☐ |
| Grafana dashboard created | ☐ |
| API traffic generated and observed | ☐ |
| All 13 screenshots in `Screenshots/` folder | ☐ |
| README updated | ☐ |
| No credentials committed to Git | ☐ |
| `.env` not committed | ☐ |
