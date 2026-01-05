# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Quarkus-based intelligent routing service that redirects users to their designated Red Hat OpenShift Dev Spaces instance based on OpenShift group membership. The application runs with an OAuth Proxy sidecar for authentication and uses the OpenShift/Kubernetes API to query group membership.

## Build and Development Commands

### Development Mode
```bash
mvn quarkus:dev
```
Starts the application in dev mode with live reload on `http://localhost:8080`

### Build
```bash
mvn clean package
```
Creates a runnable JAR at `target/quarkus-app/quarkus-run.jar`

### Run Tests
```bash
mvn test
```

### Run Single Test
```bash
mvn test -Dtest=ClassName
# Example: mvn test -Dtest=NotFoundRedirectFilterTest
```

### Build Native Executable
```bash
mvn package -Dnative
# Or with container build:
mvn package -Dnative -Dquarkus.native.container-build=true
```

### Build Container Image
```bash
mvn package -Dquarkus.container-image.build=true
```

### Deploy to OpenShift
```bash
kubectl apply -k openshift
```

### Clean Up OpenShift Resources
```bash
kubectl delete -k openshift
```

## Architecture Overview

### Request Flow
1. User hits the redirector URL (Route → Service:8443 → OAuth Proxy)
2. OAuth Proxy authenticates via OpenShift OAuth
3. OAuth Proxy injects headers (`X-Forwarded-User`, `X-Forwarded-Groups`) and forwards to Quarkus app (port 8080)
4. Quarkus app queries OpenShift API for user's group memberships
5. Application loads group-to-URL mappings from ConfigMap at `/etc/config/group-mapping.json`
6. User is redirected to the appropriate Dev Spaces instance
7. All 404 errors redirect to home page via `Redirect404Filter`

### Key Components

**GroupMappingService** (`GroupMappingService.java`)
- Reads ConfigMap from `/etc/config/group-mapping.json` on every request
- Handles Kubernetes symlink-based ConfigMap updates with retry logic
- Tracks file modification times to detect updates
- Returns group → Dev Spaces URL mappings

**OpenShiftGroupService** (`OpenShiftGroupService.java`)
- Initializes Fabric8 OpenShiftClient using service account token
- Queries OpenShift API for groups (`user.openshift.io/groups` resources)
- Methods: `getAllGroups()`, `getUserGroups(userName)`, `userBelongsToGroup(groupName, userName)`
- Requires ClusterRole with `get` and `list` permissions on groups

**Redirect404Filter** (`Redirect404Filter.java`)
- Servlet filter that intercepts all 404 responses
- Rewrites 404s to 302 redirects to `/` (home page)
- Uses `HttpServletResponseWrapper` to intercept `sendError()` and `setStatus()` calls
- Applies to all dispatcher types (REQUEST, FORWARD, INCLUDE, ERROR, ASYNC)

**Undertow Handlers** (`META-INF/undertow-handlers.conf`)
- URL rewriting rules for SPA-style routing
- Maps paths like `/f`, `/swagger`, `/dashboard` to `/index.html`

### OpenShift Deployment Architecture

**Two-container Pod:**
1. **devspaces-multicluster-redirector** (port 8080) - Quarkus application
2. **oauth-proxy** (port 8443) - OpenShift OAuth Proxy sidecar

**ConfigMap Mount:**
- ConfigMap `devspaces-openshift-group-mapping` mounted at `/etc/config`
- Contains `group-mapping.json` with group → URL mappings
- Kubernetes updates ConfigMaps via symlinks, which GroupMappingService handles

**RBAC:**
- ServiceAccount: `devspaces-multicluster-redirector`
- ClusterRole grants access to `user.openshift.io/groups` resources
- Service account token auto-mounted for OpenShift API authentication

**OAuth Proxy Configuration:**
- Provider: `openshift`
- Upstream: `http://localhost:8080`
- Passes user headers to application
- Session secret from Secret `devspaces-multicluster-redirector-session-secret`

## Important Implementation Details

### ConfigMap Hot Reload Mechanism
The `GroupMappingService` reads the ConfigMap file on every request to ensure fresh data:
- Resolves symlinks with `Path.toRealPath()` (Kubernetes uses symlinks for atomic updates)
- Tracks `lastKnownModificationTime` to detect changes
- Waits 100ms when modification time changes to let Kubernetes complete the update
- Re-resolves symlink after wait to catch final state
- Reads file as raw bytes to bypass caching

### Cache Control
All API responses include headers to prevent caching:
- `Cache-Control: no-cache, no-store, must-revalidate`
- `Pragma: no-cache`
- `Expires: 0`

This ensures users always get fresh group mappings and redirect decisions.

### OpenShift Client Initialization
The Fabric8 OpenShiftClient auto-discovers configuration:
- Uses service account token from `/var/run/secrets/kubernetes.io/serviceaccount/token`
- Detects cluster API server URL from environment
- No manual configuration needed when running in-cluster

## Technology Stack

- **Framework:** Quarkus 3.15.3.1
- **Java Version:** 17
- **Build Tool:** Maven
- **Kubernetes Client:** Fabric8 OpenShift Client 6.13.4
- **REST Framework:** Jakarta REST (JAX-RS) with Quarkus REST
- **Servlet:** Quarkus Undertow (for filter support)
- **JSON:** Jackson (quarkus-rest-jackson)
- **Testing:** JUnit 5, REST Assured

## Configuration Files

**application.properties:**
- `quarkus.http.port=8080` - HTTP port
- `quarkus.container-image.registry=quay.io` - Container registry
- `quarkus.container-image.name=redhat-developer/devspaces-multicluster-redirector` - Image name

**openshift/configmap.yaml:**
- Edit `group-mapping.json` to add/modify group → URL mappings
- Changes are auto-detected without pod restart

## REST API Endpoints

- `GET /api/group-mapping` - Returns all group-to-URL mappings (JSON)
- `GET /api/groups` - Lists all OpenShift groups
- `GET /api/userinfo` - Returns user info from OAuth headers

## Testing Notes

**GreetingResourceTest** - Basic endpoint tests
**NotFoundRedirectFilterTest** - Validates 404 → redirect behavior

When writing tests for components that use OpenShiftClient, consider mocking the client or using test profiles with mock data.
