package com.redhat.openshift.devspaces;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class GroupMappingService {

    private static final Logger LOG = Logger.getLogger(GroupMappingService.class);
    private static final String CONFIG_PATH = "/etc/config/group-mapping.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Read the ConfigMap file and return the group mapping
     * This is called on every request to ensure we always have the latest version
     * Resolves symlinks to get the actual file (Kubernetes updates ConfigMaps via symlinks)
     */
    private Map<String, String> readGroupMapping() {
        try {
            Path configPath = Paths.get(CONFIG_PATH);
            
            // Resolve symlink to get the actual file (Kubernetes uses symlinks for ConfigMap updates)
            Path realPath = configPath.toRealPath();
            
            if (Files.exists(realPath)) {
                // Force a fresh read by reading bytes and converting to string
                // This bypasses any potential caching
                byte[] bytes = Files.readAllBytes(realPath);
                String jsonContent = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                return objectMapper.readValue(jsonContent, 
                    new TypeReference<Map<String, String>>() {});
            } else {
                LOG.warnf("ConfigMap file not found at %s, using empty mapping", CONFIG_PATH);
                return new HashMap<>();
            }
        } catch (IOException e) {
            LOG.errorf(e, "Failed to read group mapping from %s", CONFIG_PATH);
            return new HashMap<>();
        }
    }

    /**
     * Get the Dev Spaces URL for a given OpenShift group
     * @param groupName The OpenShift group name
     * @return The Dev Spaces URL, or null if not found
     */
    public String getDevSpacesUrl(String groupName) {
        Map<String, String> mapping = readGroupMapping();
        return mapping.get(groupName);
    }

    /**
     * Get all group mappings
     * @return A copy of the group mapping
     */
    public Map<String, String> getAllMappings() {
        return readGroupMapping();
    }
}

