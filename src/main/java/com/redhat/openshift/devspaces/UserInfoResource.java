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

import java.util.Arrays;
import java.util.List;

@Path("/api/user")
public class UserInfoResource {

    @Inject
    GroupMappingService groupMappingService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserInfo(@Context HttpHeaders headers) {
        String user = headers.getHeaderString("X-Forwarded-User");
        String groupsHeader = headers.getHeaderString("X-Forwarded-Groups");

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode userInfo = mapper.createObjectNode();
        userInfo.put("user", user);
        userInfo.put("groups", groupsHeader);

        // Parse groups and find matching Dev Spaces URLs
        if (groupsHeader != null && !groupsHeader.isEmpty()) {
            List<String> groups = Arrays.asList(groupsHeader.split(","));
            ArrayNode devSpacesMappings = mapper.createArrayNode();
            
            for (String group : groups) {
                String trimmedGroup = group.trim();
                String devSpacesUrl = groupMappingService.getDevSpacesUrl(trimmedGroup);
                if (devSpacesUrl != null) {
                    ObjectNode mapping = mapper.createObjectNode();
                    mapping.put("group", trimmedGroup);
                    mapping.put("devSpacesUrl", devSpacesUrl);
                    devSpacesMappings.add(mapping);
                }
            }
            
            userInfo.set("devSpacesMappings", devSpacesMappings);
        }

        return Response.ok(userInfo).build();
    }
}