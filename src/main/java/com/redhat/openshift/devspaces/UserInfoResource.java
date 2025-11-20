package com.redhat.openshift.devspaces;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

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

            // Find matching Dev Spaces URLs for user's groups
            ArrayNode devSpacesMappings = mapper.createArrayNode();
            for (String group : userGroups) {
                String devSpacesUrl = groupMappingService.getDevSpacesUrl(group);
                if (devSpacesUrl != null) {
                    ObjectNode mapping = mapper.createObjectNode();
                    mapping.put("group", group);
                    mapping.put("devSpacesUrl", devSpacesUrl);
                    devSpacesMappings.add(mapping);
                }
            }
            userInfo.set("devSpacesMappings", devSpacesMappings);
        }

        return Response.ok(userInfo).build();
    }
}