[![Contribute](https://www.eclipse.org/che/contribute.svg)](https://workspaces.openshift.com#https://github.com/redhat-developer/devspaces-multicluster-redirector)
[![Contribute (nightly)](https://img.shields.io/static/v1?label=nightly%20Che&message=for%20maintainers&logo=eclipseche&color=FDB940&labelColor=525C86)](https://che-dogfooding.apps.che-dev.x6e0.p1.openshiftapps.com#https://github.com/redhat-developer/devspaces-multicluster-redirector)
[![Repository on Quay](https://quay.io/repository/redhat-developer/devspaces-multicluster-redirector/status "Repository on Quay")](https://quay.io/repository/redhat-developer/devspaces-multicluster-redirector)

# Dev Spaces Multicluster Redirector

A Quarkus-based intelligent routing service that automatically redirects users to their designated Red Hat OpenShift Dev Spaces instance based on their OpenShift group membership. This enables organizations to manage multiple Dev Spaces clusters and seamlessly route users to the appropriate instance.

## 🎯 Overview

In enterprise environments with multiple OpenShift Dev Spaces deployments across different clusters, users need to be directed to the correct instance based on their team, project, or organizational unit. This redirector service solves that problem by:

1. **Authenticating users** via OpenShift OAuth Proxy
2. **Querying OpenShift groups** to determine user membership
3. **Mapping groups to Dev Spaces URLs** using a configurable mapping
4. **Automatically redirecting** users to their designated Dev Spaces instance

## 🏗️ Architecture

### Components

- **Quarkus Application**: Lightweight Java application providing REST APIs and redirect logic
- **OAuth Proxy Sidecar**: Handles OpenShift authentication and injects user/group headers
- **OpenShift Client**: Queries the OpenShift API for group membership information
- **ConfigMap Integration**: Dynamically loads group-to-URL mappings without restart

### How It Works

```
User Request → OAuth Proxy → Quarkus App → OpenShift API → Group Mapping → Redirect
```

1. User accesses the redirector URL
2. OAuth Proxy authenticates the user against OpenShift
3. User and group information is passed via HTTP headers (`X-Forwarded-User`, `X-Forwarded-Groups`)
4. Application queries OpenShift API to get user's group memberships
5. Group mappings are loaded from ConfigMap (`/etc/config/group-mapping.json`)
6. User is redirected to the appropriate Dev Spaces instance
7. 404 errors redirect to home page for seamless user experience

## 🔑 Key Features

### Dynamic Group Mapping
- **ConfigMap-based configuration**: Update group mappings without redeploying
- **Real-time updates**: Automatically detects ConfigMap changes via symlink resolution
- **Retry mechanism**: Handles Kubernetes ConfigMap update delays gracefully

### OpenShift Integration
- **Native group queries**: Direct integration with OpenShift API
- **Service account authentication**: Uses Kubernetes service account tokens
- **RBAC support**: Requires appropriate cluster role bindings

### Smart Redirect Handling
- **404 to Home**: All 404 errors redirect to the home page
- **No caching**: Cache control headers prevent stale data
- **User-friendly**: Seamless experience with automatic redirects

## 🚀 Getting Started

### Prerequisites

- Java 17 or later
- Maven 3.9.x or later
- OpenShift cluster access (for deployment)
- Podman (for container builds)

### Running Locally in Development Mode

Development mode enables live coding with automatic reload:

```bash
mvn quarkus:dev
```

The application will start on `http://localhost:8080`

### Building the Application

#### Standard Build
Creates a runnable JAR file:

```bash
mvn clean package
```

The application produces:
- `target/quarkus-app/quarkus-run.jar` - Main executable
- `target/quarkus-app/lib/` - Dependencies

Run the application:
```bash
java -jar target/quarkus-app/quarkus-run.jar
```

#### Native Executable
Create a native executable for faster startup and lower memory footprint:

```bash
mvn package -Dnative
```

Or using a container build (no GraalVM installation required):
```bash
mvn package -Dnative -Dquarkus.native.container-build=true
```

Run the native executable:
```bash
./target/devspaces-multicluster-redirector-1.0.0-SNAPSHOT-runner
```

#### Container Image
Build a container image:

```bash
mvn package -Dquarkus.container-image.build=true
```

## 📦 Deploying to OpenShift

### Prerequisites

1. **Service Account**: The application requires a service account with permissions to list and read OpenShift groups
2. **ConfigMap**: Create a ConfigMap with group-to-URL mappings
3. **OAuth Proxy**: Configured for OpenShift authentication

### Deployment Steps

The `openshift/` directory contains Kustomize configuration files:

```bash
kubectl apply -k openshift
```

This creates:
- **ServiceAccount**: `devspaces-multicluster-redirector`
- **ClusterRole**: Permissions to list and get groups
- **ClusterRoleBinding**: Binds the role to the service account
- **ConfigMap**: Group mapping configuration
- **Secret**: OAuth proxy session secret
- **Deployment**: Application with OAuth proxy sidecar
- **Service**: Internal service on port 8443
- **Route**: External HTTPS access

### Configuration

#### Group Mapping ConfigMap

Edit `openshift/configmap.yaml` to configure your group mappings:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: devspaces-openshift-group-mapping
data:
  group-mapping.json: |
    {
      "team-alpha": "https://devspaces-alpha.example.com",
      "team-beta": "https://devspaces-beta.example.com",
      "contractors": "https://devspaces-external.example.com"
    }
```

**Note**: ConfigMap updates are automatically detected by the application without restart.

#### RBAC Permissions

The service account needs these permissions (defined in `openshift/clusterrole.yaml`):

```yaml
rules:
  - apiGroups: ["user.openshift.io"]
    resources: ["groups"]
    verbs: ["get", "list"]
```

## 🔧 Configuration

### Application Properties

Configure in `src/main/resources/application.properties`:

```properties
# HTTP port
quarkus.http.port=8080

# Container image settings
quarkus.container-image.registry=quay.io
quarkus.container-image.name=redhat-developer/devspaces-multicluster-redirector
```

### Environment Variables

The OAuth proxy sidecar uses these environment variables:
- `NAMESPACE`: Automatically injected from pod metadata

## 🧪 Testing

Run the test suite:

```bash
mvn test
```

Tests include:
- `GreetingResourceTest`: Basic endpoint testing
- `NotFoundRedirectFilterTest`: 404 redirect functionality

## 📊 Monitoring and Troubleshooting

### Logs

View application logs:
```bash
kubectl logs -f deployment/devspaces-multicluster-redirector -c devspaces-multicluster-redirector
```

View OAuth proxy logs:
```bash
kubectl logs -f deployment/devspaces-multicluster-redirector -c oauth-proxy
```

### Common Issues

1. **User not redirected**: Check if user belongs to any mapped groups
2. **ConfigMap not updating**: Verify the ConfigMap is mounted at `/etc/config`
3. **Authentication failures**: Check OAuth proxy configuration and service account permissions
4. **Group query failures**: Verify ClusterRole and ClusterRoleBinding are correctly applied

## 🛠️ Development

### Technology Stack

- **Framework**: Quarkus 3.15.3.1
- **Language**: Java 17
- **Build Tool**: Maven
- **Kubernetes Client**: Fabric8 OpenShift Client 6.13.4
- **REST**: Jakarta REST (JAX-RS)
- **JSON**: Jackson
- **Authentication**: OpenShift OAuth Proxy

## 🤝 Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

### Development Workflow

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests: `mvn test`
5. Build the project: `mvn package`
6. Submit a pull request

## 📄 License

This project is licensed under the terms specified in the [LICENSE](LICENSE) file.

## 🔗 Related Projects

- [Red Hat OpenShift Dev Spaces](https://developers.redhat.com/products/openshift-dev-spaces/overview)
- [Eclipse Che](https://www.eclipse.org/che/)
- [Quarkus](https://quarkus.io/)

## 📞 Support

For issues and questions:
- Open an issue in this repository
- Check the [Red Hat Developer documentation](https://developers.redhat.com/)
