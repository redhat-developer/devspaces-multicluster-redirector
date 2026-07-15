package com.redhat.openshift.devspaces;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class GroupMappingService {

    private static final Logger LOG = Logger.getLogger(GroupMappingService.class);
    private static final String CONFIG_PATH = "/etc/config/group-mapping.json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private FileTime lastKnownModificationTime = null;

    /**
     * Read the ConfigMap file and return the group mapping
     * This is called on every request to ensure we always have the latest version
     * Resolves symlinks to get the actual file (Kubernetes updates ConfigMaps via symlinks)
     * Includes a retry mechanism if the file appears to have been recently updated
     */
    private Map<String, String> readGroupMapping() {
        try {
            Path configPath = Paths.get(CONFIG_PATH);
            
            // Resolve symlink to get the actual file (Kubernetes uses symlinks for ConfigMap updates)
            Path realPath = configPath.toRealPath();
            
            if (Files.exists(realPath)) {
                // Check if file modification time has changed (indicates ConfigMap update)
                FileTime currentModTime = Files.getLastModifiedTime(realPath);
                
                // If we detect a recent change, wait a bit and retry to ensure Kubernetes has finished updating
                if (lastKnownModificationTime != null && 
                    currentModTime.compareTo(lastKnownModificationTime) > 0) {
                    LOG.infof("ConfigMap file modification time changed, waiting for update to complete...");
                    try {
                        Thread.sleep(100); // Small delay to let Kubernetes finish the update
                        // Re-resolve the path in case symlink changed
                        realPath = configPath.toRealPath();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                
                lastKnownModificationTime = Files.getLastModifiedTime(realPath);
                
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
     * Returns true if the user belongs to all groups listed in the given key.
     * Keys may be comma-separated (e.g. "team-alpha, team-beta"), requiring membership in all named groups.
     */
    static boolean matchesAllGroups(String groupKey, List<String> userGroups) {
        return Arrays.stream(groupKey.split(","))
                .map(String::trim)
                .filter(g -> !g.isEmpty())
                .allMatch(userGroups::contains);
    }

    /**
     * Returns all ConfigMap entries whose key (possibly a comma-separated group list)
     * matches the user's groups using AND logic. Preserves ConfigMap insertion order.
     *
     * @param userGroups the groups the user belongs to
     * @return map of matched group-key → Dev Spaces URL
     */
    public Map<String, String> getMatchingMappings(List<String> userGroups) {
        Map<String, String> allMappings = readGroupMapping();
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : allMappings.entrySet()) {
            if (matchesAllGroups(entry.getKey(), userGroups)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Get all group mappings
     * @return A copy of the group mapping
     */
    public Map<String, String> getAllMappings() {
        return readGroupMapping();
    }
}

