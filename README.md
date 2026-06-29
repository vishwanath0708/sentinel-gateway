# 🛡️ Sentinel Gateway

### High-Performance Distributed API Gateway with Adaptive Rate Limiting

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/vishwanath0708/sentinel-gateway)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.0.0--SNAPSHOT-orange.svg)]()
[![Docker Pulls](https://img.shields.io/docker/pulls/vishwanathhubballi/sentinelgateway.svg)](https://hub.docker.com/r/vishwanathhubballi/sentinelgateway)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.10-brightgreen.svg)](https://spring.io/projects/spring-boot)

**Sentinel Gateway** is an enterprise-grade, cloud-native API Gateway sidecar designed to protect downstream microservices from traffic surges, API abuse, and cascading backend failures. It integrates high-performance distributed rate limiting with a self-healing feedback loop that dynamically adjusts request consumption limits based on live system metrics.

---

## 📖 Overview

In modern microservice architectures, static rate limiters are often insufficient. A downstream database spike or third-party service delay can quickly exhaust connection pools and trigger cascading system failures, even under normal traffic conditions. Traditional gateways continue to forward requests up to their maximum static capacity, exacerbating the bottleneck.

**Sentinel Gateway** addresses this challenge by combining atomic, Redis-backed token bucket limiting with an active feedback loop. The system monitors live request latency and system error rates to calculate a real-time health score. Under resource contention, the gateway automatically throttles client request allocations (decreasing bucket refill rates by up to 75%). This dynamic self-healing capacity reduces backend pressure and gives database connections or external services room to recover.

### Key Operational Advantages:
*   **Atomicity at Scale:** Token bucket state changes are processed as a single atomic unit using in-database Lua scripting, preventing concurrency race conditions across clustered gateway nodes.
*   **Resource Resiliency:** An ultra-fast, in-memory Global QPS Guard protects the gateway itself, rejecting traffic surges instantly before querying the distributed database.
*   **Production Telemetry:** Out-of-the-box integrations with Micrometer, Prometheus, and an interactive, client-side administrator dashboard for live telemetry monitoring.

---

## 🏗️ Architecture

The request verification flow proceeds sequentially through three distinct protective layers:

```
                              +-------------------------+
                              |    Incoming Request     |
                              |  (client_id, tier_type) |
                              +------------+------------+
                                           |
                                           | HTTPS
                                           v
                              +-------------------------+
                              | 1. Global QPS Guard     | ---- [Limit Exceeded] ----> [ HTTP 429 ]
                              |  (Atomic In-Memory IP)  |
                              +------------+------------+
                                           |
                                           | [Within Safe Limits]
                                           v
                              +-------------------------+
                              | 2. Adaptive Controller  | <--- Metrics (Latency, Error Rate)
                              |  (Computes Refill Rate) |
                              +------------+------------+
                                           |
                                           | Adjusted Refill Rate
                                           v
                              +-------------------------+
                              | 3. Redis Token Bucket   |
                              |  (Atomic Lua execution) |
                              +------------+------------+
                                           |
                   +-----------------------+-----------------------+
                   |                                               |
                   v [Token Available]                             v [Exceeded / Exhausted]
       +-----------------------+                       +-----------------------+
       |   Allow Request       |                       |   Reject Request      |
       |  (Forward to Backend) |                       |   (HTTP 429 Too Many) |
       +-----------+-----------+                       +-----------------------+
                   |
                   | Response Sent (OK / Error)
                   v
       +-----------------------+
       |   Metrics Collector   |  ---> Updates 10s Rolling sliding window
       +-----------------------+
```

---

## 🔥 Features

*   🚀 **Distributed Atomic Token Bucket:** Computes token replenishment and consumption atomically in Redis via Lua scripts, avoiding transaction race conditions and dirty reads across nodes.
*   🧠 **Adaptive Throttling Engine:** Monitors average latency and system error rates via a sliding 10-second window. Under load, it dynamically scales down client capacity:
    *   **Healthy State:** Normal configured refill rate.
    *   **Latency > 200ms:** Refill rate reduced by **30%**.
    *   **Latency > 400ms:** Refill rate reduced by **50%**.
    *   **Error Rate > 5%:** Refill rate reduced by **75%** (Critical mode).
*   🔒 **Global QPS Guard:** High-speed in-memory system capacity limiter that rejects requests prior to hitting Redis during intense traffic storms, protecting infrastructure dependencies.
*   🏷️ **Dynamic Tier Policies:** Multi-tenant support with pre-packaged config profiles (`FREE`, `PRO`, `VIP`) editable at runtime without restarting gateway instances.
*   🔌 **Fail-Open/Fail-Closed Operations:** Resilient fallback engine that toggles behaviour under Redis cluster connection failure:
    *   `FAIL_OPEN`: Accepts requests to preserve service availability.
    *   `FAIL_CLOSED`: Hard-blocks requests to ensure database stability.
*   📊 **Operational Admin Console:** Clean, real-time dashboard powered by Chart.js showing global QPS trends, latency averages, and offering live controls to update policies and toggle settings.

---

## 🛠️ Tech Stack

| Technology | Version | Purpose |
| :--- | :--- | :--- |
| **Java** | 17 | Core programming language runtime |
| **Spring Boot** | 3.5.10 | Core framework (Spring MVC, Scheduling, Actuator) |
| **Redis** | 7.x | High-throughput, distributed in-memory data store |
| **Lua Scripting** | Custom | Server-side script execution inside Redis for atomicity |
| **Chart.js** | 4.4.x | Real-time chart visualization in Admin UI |
| **Tailwind CSS** | 3.x | Clean, responsive design language for the Admin console |
| **Lombok** | 1.18.x | Boilerplate code reduction utility |
| **Maven** | 3.9.x | Project build lifecycle and dependency management |

---

## 📋 Prerequisites

Before running Sentinel Gateway, ensure your local environment contains:
1.  **Java Development Kit (JDK) 17** or higher.
2.  **Apache Maven 3.8+** (or use the included wrapper `./mvnw`).
3.  **Docker & Docker Compose** (to containerize and run Redis).
4.  **curl** (or any API testing tool like Postman).

---

## 🚀 Quick Start

Get Sentinel Gateway running locally in under 5 minutes:

### 1. Clone the Repository
```bash
git clone https://github.com/vishwanath0708/sentinel-gateway.git
cd sentinel-gateway
```

### 2. Launch Redis Instance
Ensure Docker is running, then run a local Redis container on port `6379`:
```bash
docker run -d --name sentinel-redis -p 6379:6379 redis:7-alpine
```

### 3. Build & Run the Gateway
Execute the Spring Boot Maven plugin command:
```bash
./mvnw spring-boot:run
```

The gateway will start up on port **`8080`**.

### 4. Verify Rate Limiting
Send a validation request via `curl` representing a client on the `FREE` tier:
```bash
curl -X POST http://localhost:8080/api/shouldAllow \
  -H "Content-Type: application/json" \
  -d '{"clientId": "client_101", "tier": "FREE"}'
```

**Expected Response (Allowed):**
```json
{
  "allowed": true,
  "remainingTokens": 59
}
```

---

## 🔧 Installation & Setup

### Option A: Running Containerized (Docker Compose)
Create a `docker-compose.yml` file to run both Redis and Sentinel Gateway:

```yaml
version: '3.8'

services:
  redis:
    image: redis:7-alpine
    container_name: sentinel-redis
    ports:
      - "6379:6379"
    restart: always

  sentinel-gateway:
    image: vishwanathhubballi/sentinelgateway:latest
    container_name: sentinel-gateway
    ports:
      - "8080:8080"
    environment:
      - SPRING_REDIS_HOST=redis
      - SPRING_REDIS_PORT=6379
      - SPRING_REDIS_TIMEOUT=2000ms
    depends_on:
      - redis
    restart: always
```

Run the compose stack:
```bash
docker compose up -d
```

### Option B: Building from Source
Build the fat JAR package directly via Maven:
```bash
./mvnw clean package
```
Launch the compiled package:
```bash
java -jar target/smart-api-gateway-0.0.1-SNAPSHOT.jar
```

---

## ⚙️ Configuration Reference

The application can be configured via environment variables or properties defined in `src/main/resources/application.properties`:

| Property Key | Env Variable / Override | Default Value | Description |
| :--- | :--- | :--- | :--- |
| `spring.application.name` | `APP_NAME` | `Sentinel Gateway` | Registered microservice identity name |
| `spring.redis.host` | `SPRING_REDIS_HOST` | `localhost` | Redis server hostname |
| `spring.redis.port` | `SPRING_REDIS_PORT` | `6379` | Port number of active Redis server |
| `spring.redis.timeout` | `SPRING_REDIS_TIMEOUT` | `2000ms` | Network socket read timeout for Redis commands |
| `server.port` | `PORT` | `8080` | Port on which the Spring Boot application listens |

---

## 📖 Usage Guide

Sentinel Gateway works as a standalone check-and-allow service. To integrate it with your API workflow, use one of the following patterns:

### 1. Integration Patterns

#### Pattern 1: Node.js / Express Middleware
Add this middleware to route checks before invoking downstream controllers:
```javascript
const axios = require('axios');

async function sentinelRateLimit(req, res, next) {
  try {
    const response = await axios.post('http://localhost:8080/api/shouldAllow', {
      clientId: req.ip || req.headers['x-user-id'],
      tier: req.headers['x-tier'] || 'FREE'
    });
    
    res.setHeader('X-RateLimit-Remaining', response.data.remainingTokens);
    next();
  } catch (error) {
    if (error.response && error.response.status === 429) {
      return res.status(429).json({ error: 'Rate Limit Exceeded. Backoff.' });
    }
    // Fail-open strategy if gateway is unreachable
    next();
  }
}
```

#### Pattern 2: Sidecar Container Configuration
In Kubernetes, run Sentinel Gateway in the same pod alongside your main application. Your main application can communicate with it over `localhost:8080` to evaluate incoming API keys before doing heavy calculations.

---

## 🔌 API Documentation

Sentinel Gateway exposes both **Client Verification** endpoints and **Administrator Control** endpoints.

### Client API

#### Check Rate Limit Allocation
*   **Path:** `POST /api/shouldAllow`
*   **Content-Type:** `application/json`
*   **Request Body:**
    ```json
    {
      "clientId": "user_id_99",
      "tier": "PRO"
    }
    ```
*   **Success Response (200 OK):**
    ```json
    {
      "allowed": true,
      "remainingTokens": 599
    }
    ```
*   **Rejected Response (429 Too Many Requests):**
    ```json
    {
      "allowed": false,
      "remainingTokens": 0
    }
    ```

---

### Admin Dashboard API

#### 1. Retrieve Live Telemetry
*   **Path:** `GET /admin/metrics`
*   **Response (200 OK):**
    ```json
    {
      "totalAllowed": 14205,
      "totalRejected": 124,
      "globalQps": 42,
      "avgLatencyMs": 14.5,
      "errorRatePercent": 0.23,
      "failureMode": "FAIL_CLOSED"
    }
    ```

#### 2. Get Tier Policy Configurations
*   **Path:** `GET /admin/policies`
*   **Response (200 OK):**
    ```json
    {
      "FREE": { "capacity": 60, "refillRatePerSecond": 1 },
      "PRO": { "capacity": 600, "refillRatePerSecond": 100 },
      "VIP": { "capacity": 6000, "refillRatePerSecond": 1000 }
    }
    ```

#### 3. Update Policy Config
*   **Path:** `PUT /admin/policies/{tier}`
*   **Request Body:**
    ```json
    {
      "capacity": 80,
      "refillRate": 2
    }
    ```
*   **Response (200 OK):** Empty Body.

#### 4. Configure Global QPS Limit
*   **Path:** `POST /admin/global-qps?limit=15000`
*   **Response (200 OK):** Empty Body.

#### 5. Set System Failure Strategy Mode
*   **Path:** `POST /admin/failure-mode?mode=FAIL_OPEN`
*   **Response (200 OK):** Empty Body. (Toggles between `FAIL_OPEN` and `FAIL_CLOSED`).

---

## 📂 Project Structure

```
sentinel-gateway/
├── Dockerfile                  # Multi-stage image build script
├── Jenkinsfile                 # Jenkins pipeline automation script
├── pom.xml                     # Maven build and dependencies
├── Document.txt                # Engineering build phase guidelines
├── src/
│   ├── main/
│   │   ├── java/com/smart_api_gateway/
│   │   │   ├── SentinelGatewayApplication.java # Spring Boot Entrypoint
│   │   │   ├── config/
│   │   │   │   ├── CorsConfig.java           # Cross-Origin resource mappings
│   │   │   │   └── RedisScriptConfig.java     # Loads & registers Lua bucket script
│   │   │   ├── controller/
│   │   │   │   ├── AdminController.java       # System status & config endpoints
│   │   │   │   └── RateLimiterController.java # Rate limit verification entrypoint
│   │   │   ├── limiter/
│   │   │   │   └── TokenBucket.java           # Pure Java in-memory fallback bucket
│   │   │   ├── model/
│   │   │   │   ├── RateLimitRequest.java      # Client input DTO
│   │   │   │   ├── RateLimitResponse.java     # Verification result DTO
│   │   │   │   └── RateLimitResult.java       # Internal result record DTO
│   │   │   ├── monitor/
│   │   │   │   ├── AdaptiveMetricsCollector.java # Monitors telemetry stats
│   │   │   │   └── AdaptiveRateController.java  # Scales refill rates
│   │   │   ├── policy/
│   │   │   │   ├── ClientTier.java            # Client tier enum (FREE, PRO, VIP)
│   │   │   │   ├── FailureMode.java           # Failure enum (OPEN, CLOSED)
│   │   │   │   └── RateLimitPolicy.java       # Capacity and refill definitions
│   │   │   └── service/
│   │   │       └── RateLimiterService.java    # Orchestrates script checks
│   │   └── resources/
│   │       ├── application.properties         # Main properties configuration
│   │       └── static/
│   │           └── dashboard.html             # Chart & Control admin dashboard
│   └── test/
│       └── java/com/smart_api_gateway/
│           └── SentinelGatewayApplicationTests.java # Context loading test suite
```

---

## 💻 Development & Contributions

### Local Development Environment Setup
Import the project into IntelliJ IDEA, Eclipse, or VS Code as a Maven project. Enable annotation processing in your IDE settings so that **Lombok** can compile the project properly.

### Running Test Suite
Execute unit and integration tests:
```bash
./mvnw test
```

### Code Styling & Standards
Ensure compliance with standard Java formatting rules:
*   Do not leave commented-out code blocks.
*   Add descriptive logs via Lombok's `@Slf4j` when implementing new modules.
*   Ensure all new API responses map structured models to JSON keys using camelCase syntax.

---

## 🐛 Troubleshooting

### 1. Redis Connection Timeouts
**Issue:** Gateway throws `QueryTimeoutException` or `RedisConnectionFailureException`.
*   **Fix:** Ensure your local Redis server is active by running `docker ps`. If running on a non-standard port or hostname, update `spring.redis.host` and `spring.redis.port` in your `application.properties`.

### 2. CORS Blockage in Dashboard
**Issue:** The Admin dashboard cannot query metrics or update policies.
*   **Fix:** By default, `CorsConfig.java` is enabled to allow all origins (`*`) for development purposes. Check if your request passes through an API proxy that strips CORS headers.

### 3. Lua Script Execution Errors
**Issue:** Redis throws `NOSCRIPT No matching script` or Lua script compilation error.
*   **Fix:** Ensure you are running Redis v6.2+. If Redis is restarted during runtime, Spring Data Redis will automatically re-load the Lua script on the next check.

---

## ⚡ Performance & Optimization

For high-throughput production workloads, apply the following optimizations:
1.  **Connection Pooling:** Configure Jedis/Lettuce connection pool values in `application.properties` to avoid thread blocking:
    ```properties
    spring.data.redis.lettuce.pool.max-active=64
    spring.data.redis.lettuce.pool.max-idle=16
    spring.data.redis.lettuce.pool.min-idle=8
    ```
2.  **Redis Pipelining:** If querying rate limits sequentially, use Redis pipeline batches to reduce network round trips.
3.  **Kernel Tweaks:** Increase open file descriptors (`ulimit -n 65536`) on the host server hosting the gateway to allow high socket densities.

---

## 🔒 Security Considerations

*   **Secure Admin Endpoints:** In production, restrict `/admin/*` paths using Spring Security, limiting access to users with basic authorization or JWT certificates.
*   **CORS Hardening:** Modify `CorsConfig.java` from wildcard `*` to explicitly allowed origins (e.g., your admin panel domain name).
*   **Rate Limit Bypass Protection:** Ensure client identification is resolved via secure HTTP headers configured in upstream load balancers (e.g., `X-Forwarded-For`), preventing IP address spoofing.

---

## 🗺️ Roadmap

- [ ] **Dynamic DB Backing:** Integrate PostgreSQL via Spring Data JPA to store tenant metadata permanently.
- [ ] **JWT Rate Limit Extraction:** Extract client identities and subscription tiers directly from incoming JWT tokens.
- [ ] **IP Whitelisting & Blacklisting:** Add real-time CIDR block matching filters to drop malicious traffic instantly.
- [ ] **Prometheus Exporter:** Native prometheus metric endpoints for automated alert routing.

---

## 🤝 Contributing Guidelines

We welcome community feedback and contributions!
1.  Fork the repository.
2.  Create your feature branch (`git checkout -b feature/CoolFeature`).
3.  Commit your modifications with concise summaries (`git commit -m 'Add support for CoolFeature'`).
4.  Push the branch (`git push origin feature/CoolFeature`).
5.  Open a Pull Request targeting the `main` branch.

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

---

## 💡 Acknowledgments

*   The Spring Boot Team for the web infrastructure.
*   Redis for the outstanding atomic transaction execution capability.
*   Bootstrap and Chart.js contributors for the UI tools.
