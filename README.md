# Cirrina Dapr Smart Factory

A Smart Factory application implemented using **Dapr Actors**, together with the supporting components used to execute and benchmark the application in a distributed Dapr deployment.

The repository models a distributed part-assembly workflow in which multiple actors coordinate through events and invoke external services for simulated physical operations. It also includes benchmark utilities for configurable workload generation and experiment-level metric collection.

## Overview

The Smart Factory application is composed of the following logical components:

| Component | Responsibility |
| --- | --- |
| Job Controller | Coordinates the overall production workflow and tracks completed products. |
| Conveyor Belt | Models transport of parts through the factory. |
| Robotic Arm | Performs pickup, assembly, and reset operations. |
| Assembly Controller | Coordinates part detection, image capture/scanning, and assembly-related actions. |
| Message Processor | Handles application messages such as email and SMS notifications. |
| Monitor | Collects application-level statistics. |

The application logic is implemented using Dapr Actors and Spring Boot. The benchmark support layer contains three additional Kotlin/JVM applications:

- **Smart Factory Service** — provides the HTTP endpoints invoked by the actors and publishes asynchronous peripheral events through Dapr pub/sub.
- **Metrics Collector** — observes benchmark lifecycle events and records production-time and observed-arrival-rate data.
- **Event Publisher** — generates incoming part-arrival events according to a configurable workload.

The Smart Factory application, service, metrics collector, and event publisher are built as Gradle subprojects and containerized with Docker.

## Repository Structure

```text
.
├── api/                              # Shared request/serialization API
├── buildSrc/                         # Shared Gradle conventions
├── smartFactory/                     # Dapr Smart Factory application
├── benchmark-s1/
│   ├── smart-factory-service/        # External Smart Factory services
│   ├── metrics-collector/            # Experiment-level metric collection
│   └── event-publisher/              # Configurable workload generator
├── gradle/
├── gradlew
├── build.gradle.kts
└── settings.gradle.kts
```

The project uses a root Gradle multi-project build. Shared Kotlin, Spring Boot, Dapr, formatting, and Java configuration is provided through the `common-conventions` Gradle convention plugin.

## Technology Stack

- Kotlin / JVM 25
- Gradle
- Spring Boot
- Dapr Actors
- Dapr pub/sub
- Apache Fory
- ONNX Runtime
- Dropwizard Metrics
- Docker

## Prerequisites

For local development:

- JDK 25
- Docker
- The Gradle wrapper included in the repository

For running the complete application:

- Dapr runtime and sidecars
- A Dapr placement service for actors
- Configured Dapr state-store and pub/sub components
- Network connectivity between application components and sidecars

## Building the Project

The repository is a Gradle multi-project build. From the repository root:

```bash
./gradlew build
```

To build the smart factory application:

```bash
./gradlew smartFactory:build
```

To build individual benchmark components:

```bash
./gradlew :benchmark-s1:smart-factory-service:build
./gradlew :benchmark-s1:metrics-collector:build
./gradlew :benchmark-s1:event-publisher:build
```

## Building the Docker Images

The Dockerfiles use multi-stage builds. All images are built using the repository root as the Docker build context so that the root Gradle configuration, shared convention plugin, and shared modules are available during the build.

### Smart Factory

```bash
docker build \
  -f smartFactory/Dockerfile \
  -t smartfactory:dapr \
  .
```

### Smart Factory Service

```bash
docker build \
  -f benchmark-s1/smart-factory-service/Dockerfile \
  -t smartfactoryservice:dapr \
  .
```

### Metrics Collector

```bash
docker build \
  -f benchmark-s1/metrics-collector/Dockerfile \
  -t smartfactorycollector:dapr \
  .
```

### Event Publisher

```bash
docker build \
  -f benchmark-s1/event-publisher/Dockerfile \
  -t smartfactoryep:dapr \
  .
```

## Runtime Configuration

### Smart Factory Application

Each Smart Factory process runs a role-specific actor or subscriber configuration.

Common runtime variables include:

| Variable | Description |
| --- | --- |
| `ROLE` | Selects the Smart Factory role executed by the process. |
| `ACTOR_ID` | Actor instance identifier used by the process. |
| `SERVICE_URL` | URL of the Smart Factory service. |
| `DAPR_HTTP_ENDPOINT` | HTTP endpoint of the local Dapr sidecar. |
| `DAPR_GRPC_ENDPOINT` | gRPC endpoint of the local Dapr sidecar. |

### Smart Factory Service

The service listens on port `6000` and exposes only the endpoints required for the selected role.

| Variable | Required | Description |
| --- | --- | --- |
| `SERVICE_ROLE` | Yes | Selects the endpoints provided by the process. Supported values are `monitor`, `mp`, `belt`, `arm`, and `ac`. |
| `DAPR_HTTP_ENDPOINT` | Yes | HTTP endpoint of the local Dapr sidecar. |
| `DAPR_GRPC_ENDPOINT` | Yes | gRPC endpoint of the local Dapr sidecar. |

### Metrics Collector

The metrics collector observes benchmark lifecycle events and records experiment-level timing data. Its output is written under:

```text
/metrics
```

### Event Publisher

The event publisher generates incoming part-arrival events according to the configured workload and stops after the production workflow completes.

Typical configuration includes:

| Variable | Description |
| --- | --- |
| `PART_ARRIVAl_RATE_PER_SEC` | Configured part-arrival rate in events per second. |
| `PUBLISH_START_DELAY` | Delay before workload generation starts, in milliseconds. |
| `PUBLISH_MODE` | Arrival-generation mode. |
| `DAPR_HTTP_ENDPOINT` | HTTP endpoint of the local Dapr sidecar. |
| `DAPR_GRPC_ENDPOINT` | gRPC endpoint of the local Dapr sidecar. |

## Running the Complete Application

The benchmark applications are not standalone. A complete deployment additionally requires the Dapr runtime infrastructure and sidecars.

At runtime, the Smart Factory roles correspond to:

```text
messageProcessor
jobController
belt
arm
monitor
assemblyController
```

A typical deployment consists of:

1. Dapr placement and required component infrastructure.
2. One Dapr sidecar for each Smart Factory application or benchmark-support process.
3. The role-specific Smart Factory applications.
4. The Smart Factory service processes.
5. The metrics collector.
6. The event publisher.

For distributed benchmark runs, ensure that all nodes use synchronized system clocks before collecting cross-node latency measurements.

## Benchmark Flow

At a high level, an experiment proceeds as follows:

1. Start the Dapr infrastructure and configured components.
2. Start the Dapr sidecars and Smart Factory actor processes.
3. Start the role-specific Smart Factory service processes.
4. Start the metrics collector.
5. Start the event publisher with the desired arrival rate.
6. The publisher emits incoming part-arrival events.
7. Actors coordinate the production workflow and invoke external services.
8. The collector records the production interval between `eProductionStarted` and `eJobDone`.
9. The publisher stops after observing `eJobDone`.
10. Metrics are flushed and collected for analysis.

The benchmark separates configurable workload generation from the application itself so that different arrival rates can be evaluated without modifying the Smart Factory implementation.

## Development

### Formatting

The Kotlin codebase uses `ktfmt` with Google style.

From the repository root:

```bash
./gradlew ktfmtFormat
```

To check formatting without modifying files:

```bash
./gradlew ktfmtCheck
```

To format or check an individual benchmark component:

```bash
./gradlew :benchmark-s1:smart-factory-service:ktfmtFormat
./gradlew :benchmark-s1:smart-factory-service:ktfmtCheck
```

The same task pattern can be used for the metrics collector and event publisher.

### Shared API

The `api` module contains data structures and serialization configuration shared by components that exchange application-specific request data.

The Smart Factory service and event publisher depend on this shared module through the root Gradle build.

### Shared Build Conventions

Common build configuration is defined in `buildSrc` through the `common-conventions` plugin. Benchmark subprojects use this convention to share Java, Kotlin, Spring Boot, Dapr, dependency, and formatting configuration.

## CI and Docker Publishing

GitHub Actions are used to:

- check Kotlin formatting,
- build and test the Gradle multi-project codebase,
- publish JUnit test reports, and
- build and publish Docker images for the Smart Factory application and benchmark components.

## Acknowledgements

This repository provides the **Dapr baseline implementation** used to evaluate the Collaborative State Machine (CSM) approach and the Cirrina runtime developed at the University of Innsbruck.
