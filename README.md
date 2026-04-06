# 🚀 Smart API Gateway with Adaptive Distributed Rate Limiting

An advanced API Gateway implementing:

- 🔒 Distributed per-user rate limiting (Redis + Lua)
- 🌍 Global system-level QPS protection
- 🧠 Adaptive refill rate based on latency & error rate
- 📊 Sliding health window monitoring
- 🏷 Multi-tier policy support (FREE / PRO / VIP)

---

## 🏗 Architecture

```
Client
   ↓
Global QPS Guard (In-Memory)
   ↓
Adaptive Rate Controller
   ↓
Redis Lua Token Bucket (Atomic Execution)
   ↓
Backend Service
```

---

## 🔥 Features

### 1️⃣ Distributed Token Bucket
- Implemented using Redis + Lua
- Atomic execution inside Redis
- No race conditions
- TTL-based memory cleanup
- One Redis call per request

### 2️⃣ Global Protection Layer
- In-memory QPS limiter
- Fast rejection before hitting Redis
- Protects backend from traffic storms

### 3️⃣ Adaptive Throttling
- Monitors:
    - Average Latency
    - Error Rate
- Dynamically adjusts refill rate
- Prevents cascading system failures
- Uses rolling 10-second health window

### 4️⃣ Multi-Tier Support

| Tier | Capacity | Refill Rate |
|------|----------|------------|
| FREE | 60       | 1/sec      |
| PRO  | 600      | 100/sec    |
| VIP  | 6000     | 1000/sec   |

---

## 🛠 Tech Stack

- Java 21
- Spring Boot
- Redis (Docker)
- Lua scripting
- AtomicLong concurrency control
- Scheduled health monitoring

---

## 🚀 How to Run

### 1️⃣ Start Redis (Docker)

```bash
docker run -d -p 6379:6379 redis
```

### 2️⃣ Run Spring Boot Application

```bash
mvn spring-boot:run
```

### 3️⃣ Test API

**Endpoint:**

```
POST /api/shouldAllow
```

**Example Request Body:**

```json
{
  "clientId": "user1",
  "tier": "PRO"
}
```

---

## 📊 Adaptive Logic

| Condition | Action |
|-----------|--------|
| Healthy | Normal refill rate |
| Latency > 200ms | Reduce refill by 30% |
| Latency > 400ms | Reduce refill by 50% |
| Error Rate > 5% | Reduce refill by 75% |

---

## 🧠 How It Works

1. Global limiter checks system-wide QPS.
2. Adaptive controller reads latency & error metrics.
3. Refill rate is dynamically adjusted.
4. Redis Lua script executes token bucket atomically.
5. Request is allowed or rejected safely.

---

## 🎯 Learning Outcomes

- Distributed systems design
- Atomic operations using Redis Lua
- Adaptive control system principles
- Backend traffic protection strategies
- Multi-layer rate limiting architecture
- Production-style gateway design


---

## 👨‍💻 Author

Built as an advanced backend infrastructure project to demonstrate system design, distributed rate limiting, and adaptive traffic control mechanisms.