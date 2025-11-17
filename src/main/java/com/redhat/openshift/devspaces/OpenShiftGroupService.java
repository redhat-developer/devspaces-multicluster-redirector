package com.redhat.openshift.devspaces;

import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.api.model.Group;
import io.fabric8.openshift.api.model.GroupList;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class OpenShiftGroupService {

    private static final Logger LOG = Logger.getLogger(OpenShiftGroupService.class);
    private final OpenShiftClient openshiftClient;

    public OpenShiftGroupService() {
        // Initialize OpenShift client using service account token
        // The client will automatically use the service account token from /var/run/secrets/kubernetes.io/serviceaccount/token
        this.openshiftClient = new KubernetesClientBuilder().build().adapt(OpenShiftClient.class);
    }

    /**
     * Get all OpenShift groups from the cluster
     * @return List of all group names
     */
    public List<String> getAllGroups() {
        try {
            GroupList groupList = openshiftClient.groups().list();
            return groupList.getItems().stream()
                    .map(group -> group.getMetadata().getName())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.errorf(e, "Failed to list OpenShift groups");
            return new ArrayList<>();
        }
    }

    /**
     * Check if a user belongs to a specific group
     * @param groupName The group name to check
     * @param userName The user name to check
     * @return true if user belongs to the group, false otherwise
     */
    public boolean userBelongsToGroup(String groupName, String userName) {
        try {
            Group group = openshiftClient.groups().withName(groupName).get();
            if (group == null) {
                return false;
            }
            
            // Check if user is in the group's users list
            List<String> users = group.getUsers();
            return users != null && users.contains(userName);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to check if user %s belongs to group %s", userName, groupName);
            return false;
        }
    }

    /**
     * Get all groups that a user belongs to
     * @param userName The user name
     * @return List of group names the user belongs to
     */
    public List<String> getUserGroups(String userName) {
        try {
            GroupList groupList = openshiftClient.groups().list();
            return groupList.getItems().stream()
                    .filter(group -> {
                        List<String> users = group.getUsers();
                        return users != null && users.contains(userName);
                    })
                    .map(group -> group.getMetadata().getName())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.errorf(e, "Failed to get groups for user %s", userName);
            return new ArrayList<>();
        }
    }
}

