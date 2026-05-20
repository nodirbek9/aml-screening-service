# =============================================================================
# Multi-stage Dockerfile for AML Screening Service
# Suitable for Fly.io, Koyeb, Render, Railway, etc.
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1 — Build the fat jar inside the container (no local ./gradlew build needed)
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# 1) Copy only the wrapper + manifests first to leverage Docker layer caching.
#    Dependencies will be downloaded only when build.gradle / settings.gradle change.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

# Make wrapper executable (in case the executable bit is lost on some systems)
RUN chmod +x gradlew

# 2) Pre-fetch dependencies (cached layer). The "|| true" lets it succeed even if
#    no real task runs at this point — we only want the dependency cache populated.
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# 3) Now copy the source and build the bootable jar (skip tests for faster deploys)
COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon


# -----------------------------------------------------------------------------
# Stage 2 — Minimal runtime image (only the JRE + the jar)
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Run as a non-root user (good practice)
RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

# Copy the jar produced in stage 1 (use a wildcard to avoid hardcoding the version)
COPY --from=build /app/build/libs/*.jar app.jar

# Memory-friendly defaults for free tiers (Fly 256–512 MB, Koyeb 512 MB).
# - MaxRAMPercentage caps heap relative to container memory limit.
# - SerialGC has the smallest footprint for tiny heaps.
# These can be overridden by the platform via JAVA_TOOL_OPTIONS env var.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"

# Expose the port (Fly.io / Koyeb will map it via PORT env)
EXPOSE 7777

USER app

ENTRYPOINT ["sh", "-c", "java $JAVA_TOOL_OPTIONS -jar /app/app.jar"]
