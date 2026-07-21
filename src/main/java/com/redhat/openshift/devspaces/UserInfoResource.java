package com.redhat.openshift.devspaces;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

@Path("/api/user")
public class UserInfoResource {

    @Inject
    GroupMappingService groupMappingService;

    @Inject
    OpenShiftGroupService openShiftGroupService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserInfo(@Context HttpHeaders headers) {
        String userHeader = headers.getHeaderString("X-Forwarded-User");
        String groupsHeader = headers.getHeaderString("X-Forwarded-Groups");

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode userInfo = mapper.createObjectNode();
        userInfo.put("user", userHeader);
        userInfo.put("groups", groupsHeader);

        // Check which OpenShift groups the user belongs to and match with ConfigMap
        if (userHeader != null && !userHeader.isEmpty()) {
            List<String> userGroups = openShiftGroupService.getUserGroups(userHeader);
            ArrayNode userGroupsArray = mapper.createArrayNode();
            for (String group : userGroups) {
                userGroupsArray.add(group);
            }
            userInfo.set("userOpenShiftGroups", userGroupsArray);

            // Find matching Dev Spaces URLs using AND logic for comma-separated group keys
            ArrayNode devSpacesMappings = mapper.createArrayNode();
            Map<String, String> matchingMappings = groupMappingService.getMatchingMappings(userGroups);
            for (Map.Entry<String, String> entry : matchingMappings.entrySet()) {
                ObjectNode mapping = mapper.createObjectNode();
                mapping.put("groupKey", entry.getKey());
                mapping.put("devSpacesUrl", entry.getValue());
                devSpacesMappings.add(mapping);
            }
            userInfo.set("devSpacesMappings", devSpacesMappings);
        }

        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        cacheControl.setNoStore(true);
        cacheControl.setMustRevalidate(true);

        return Response.ok(userInfo)
                .cacheControl(cacheControl)
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .build();
    }
}