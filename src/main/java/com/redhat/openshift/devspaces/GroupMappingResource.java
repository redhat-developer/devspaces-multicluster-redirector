package com.redhat.openshift.devspaces;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/api/group-mapping")
public class GroupMappingResource {

    @Inject
    GroupMappingService groupMappingService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroupMapping() {
        Map<String, String> mapping = groupMappingService.getAllMappings();
        return Response.ok(mapping).build();
    }
}

