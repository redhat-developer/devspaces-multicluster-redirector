package com.redhat.openshift.devspaces;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.KeyToPath;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.fabric8.openshift.api.model.Route;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Guards the custom certificate wiring in the OpenShift manifests: the injected
 * trusted CA bundle consumed by the OAuth Proxy sidecar, and the opt-in overlay
 * that serves the Route with a custom certificate.
 */
class CustomCertificateManifestTest {

    private static final String TRUSTED_CA_CONFIGMAP = "devspaces-multicluster-redirector-trusted-ca";
    private static final String CA_BUNDLE_KEY = "ca-bundle.crt";
    private static final String SERVICE_ACCOUNT_CA = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";
    private static final String OAUTH_PROXY_CONTAINER = "oauth-proxy";
    private static final String TRUSTED_CA_VOLUME = "trusted-ca";

    private static final Pattern OPENSHIFT_CA_FLAG = Pattern.compile("-openshift-ca=([^\\s\\\\]+)");

    private static final KubernetesSerialization SERIALIZATION = new KubernetesSerialization();

    // --- Injected CA bundle ConfigMap ------------------------------------------------

    @Test
    void trustedCaConfigMap_isLabelledForInjection() {
        ConfigMap configMap = load("openshift/trusted-ca-configmap.yaml", ConfigMap.class);

        assertEquals(TRUSTED_CA_CONFIGMAP, configMap.getMetadata().getName());
        assertEquals("true", configMap.getMetadata().getLabels().get("config.openshift.io/inject-trusted-cabundle"),
                "the Cluster Network Operator only injects the bundle when this label is present");
    }

    @Test
    void trustedCaConfigMap_hasNoData() {
        ConfigMap configMap = load("openshift/trusted-ca-configmap.yaml", ConfigMap.class);

        assertTrue(configMap.getData() == null || configMap.getData().isEmpty(),
                "the bundle is owned by the Cluster Network Operator; shipping data would fight the injection");
    }

    @Test
    void trustedCaConfigMap_isPartOfTheBaseKustomization() throws IOException {
        String kustomization = read("openshift/kustomization.yaml");

        assertTrue(kustomization.contains("trusted-ca-configmap.yaml"),
                "the ConfigMap must be deployed, otherwise the sidecar's volume never mounts");
    }

    // --- Sidecar wiring ---------------------------------------------------------------

    @Test
    void trustedCaVolume_projectsTheInjectedBundle() {
        Volume volume = trustedCaVolume();

        assertNotNull(volume.getConfigMap(), "the bundle comes from a ConfigMap, not another volume source");
        assertEquals(TRUSTED_CA_CONFIGMAP, volume.getConfigMap().getName());

        List<KeyToPath> items = volume.getConfigMap().getItems();
        assertEquals(1, items.size(), "only the CA bundle key should be projected");
        assertEquals(CA_BUNDLE_KEY, items.get(0).getKey(), "this is the key the operator injects");
    }

    @Test
    void oauthProxy_mountsTheInjectedBundleReadOnly() {
        VolumeMount mount = trustedCaMount(container(OAUTH_PROXY_CONTAINER));

        assertNotNull(mount, "the sidecar must see the bundle it is pointed at");
        assertEquals(Boolean.TRUE, mount.getReadOnly(), "the sidecar never writes to the bundle");
    }

    @Test
    void oauthProxy_trustsBothTheInjectedBundleAndTheServiceAccountCa() {
        List<String> caFlags = openShiftCaFlags(container(OAUTH_PROXY_CONTAINER));

        assertTrue(caFlags.contains(injectedBundlePath()),
                "expected the injected bundle at " + injectedBundlePath() + " but found " + caFlags);
        assertTrue(caFlags.contains(SERVICE_ACCOUNT_CA),
                "-openshift-ca replaces its default rather than adding to it, so the service account CA "
                        + "must be listed explicitly; found " + caFlags);
    }

    @Test
    void injectedBundlePath_matchesTheMount() {
        // A rename on either side silently stops the sidecar from trusting the custom CA:
        // a missing -openshift-ca file is tolerated at startup and only fails at login time.
        String flagged = openShiftCaFlags(container(OAUTH_PROXY_CONTAINER)).stream()
                .filter(path -> !path.equals(SERVICE_ACCOUNT_CA))
                .findFirst()
                .orElseGet(() -> fail("no -openshift-ca flag points at the injected bundle"));

        assertEquals(injectedBundlePath(), flagged);
    }

    @Test
    void applicationContainer_doesNotMountTheBundle() {
        // The Fabric8 client uses the in-cluster config, which already trusts the service account CA.
        assertNull(trustedCaMount(container("devspaces-multicluster-redirector")),
                "the application needs no CA bundle; mounting it would imply otherwise");
    }

    // --- Custom Route certificate overlay ---------------------------------------------

    @Test
    void baseRoute_usesTheDefaultIngressCertificate() {
        Route route = load("openshift/route.yaml", Route.class);

        assertEquals("edge", route.getSpec().getTls().getTermination());
        assertNull(route.getSpec().getTls().getCertificate(),
                "the base Route is served with the cluster's default ingress certificate");
    }

    @Test
    void overlay_patchesTheBase() throws IOException {
        String kustomization = read("overlays/custom-route-cert/kustomization.yaml");

        assertTrue(kustomization.contains("../../openshift"), "the overlay must build on the base manifests");
        assertTrue(kustomization.contains("route-tls-patch.yaml"), "the overlay must apply the TLS patch");
        assertTrue(Files.exists(path("overlays/custom-route-cert/route-tls-patch.yaml")));
    }

    @Test
    void overlayPatch_targetsTheRouteAndSuppliesACertificate() {
        Route patch = load("overlays/custom-route-cert/route-tls-patch.yaml", Route.class);
        Route base = load("openshift/route.yaml", Route.class);

        assertEquals(base.getMetadata().getName(), patch.getMetadata().getName(),
                "a name mismatch makes kustomize create a second Route instead of patching this one");
        assertFalse(patch.getSpec().getHost().isBlank(), "the certificate must be valid for spec.host");
        assertEquals("edge", patch.getSpec().getTls().getTermination(),
                "the certificate is terminated at the router, matching the base Route");
        assertNotNull(patch.getSpec().getTls().getCertificate());
        assertNotNull(patch.getSpec().getTls().getKey());
    }

    // --- Helpers -----------------------------------------------------------------------

    private static Deployment deployment() {
        return load("openshift/deployment.yaml", Deployment.class);
    }

    private static Container container(String name) {
        return deployment().getSpec().getTemplate().getSpec().getContainers().stream()
                .filter(c -> name.equals(c.getName()))
                .findFirst()
                .orElseGet(() -> fail("no container named " + name));
    }

    private static Volume trustedCaVolume() {
        return deployment().getSpec().getTemplate().getSpec().getVolumes().stream()
                .filter(v -> TRUSTED_CA_VOLUME.equals(v.getName()))
                .findFirst()
                .orElseGet(() -> fail("no volume named " + TRUSTED_CA_VOLUME));
    }

    private static VolumeMount trustedCaMount(Container container) {
        return container.getVolumeMounts().stream()
                .filter(m -> TRUSTED_CA_VOLUME.equals(m.getName()))
                .findFirst()
                .orElse(null);
    }

    /** Where the projected bundle actually lands in the sidecar's filesystem. */
    private static String injectedBundlePath() {
        String mountPath = trustedCaMount(container(OAUTH_PROXY_CONTAINER)).getMountPath();
        String projected = trustedCaVolume().getConfigMap().getItems().get(0).getPath();
        return mountPath.replaceAll("/$", "") + "/" + projected;
    }

    private static List<String> openShiftCaFlags(Container container) {
        String command = String.join("\n", container.getCommand());
        Matcher matcher = OPENSHIFT_CA_FLAG.matcher(command);
        return matcher.results().map(r -> r.group(1)).collect(Collectors.toList());
    }

    private static <T> T load(String relativePath, Class<T> type) {
        try (InputStream in = Files.newInputStream(path(relativePath))) {
            return SERIALIZATION.unmarshal(in, type);
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + relativePath, e);
        }
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(path(relativePath));
    }

    private static Path path(String relativePath) {
        return Paths.get(System.getProperty("basedir", ".")).resolve(relativePath);
    }
}
