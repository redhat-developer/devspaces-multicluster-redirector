[![Contribute](https://www.eclipse.org/che/contribute.svg)](https://workspaces.openshift.com#https://github.com/redhat-developer/devspaces-multicluster-redirector)
[![Repository on Quay](https://quay.io/repository/redhat-developer/devspaces-multicluster-redirector/status "Repository on Quay")](https://quay.io/repository/redhat-developer/devspaces-multicluster-redirector)

# Dev Spaces Multicluster Redirector

This project is a simple Quarkus application that serves as a redirector for Dev Spaces in a multicluster environment.

## Running the application in development mode

You can run your application in dev mode that enables live coding using:
```bash
mvn quarkus:dev
```

## Packaging and running the application

The application can be packaged using:
```bash
mvn package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an “über-jar” as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

## Creating a native executable

You can create a native executable using:
```bash
mvn package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:
```bash
mvn package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/devspaces-multicluster-redirector-1.0.0-SNAPSHOT-runner`

## Building a container image

To build a container image, you can use the following command:
```bash
mvn package -Dquarkus.container-image.build=true
```

This will build a container image using the default container runtime (e.g., Docker or Podman).

## Deploying to OpenShift

The `openshift` directory contains a Kustomize configuration for deploying the application to OpenShift with an OAuth Proxy sidecar.

To deploy the application, run the following command from the root of the project:

```bash
kubectl apply -k openshift
```
