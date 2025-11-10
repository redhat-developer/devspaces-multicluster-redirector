package com.redhat.openshift.devspaces;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.CacheControl;
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
        
        // Set cache control headers to prevent caching
        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        cacheControl.setNoStore(true);
        cacheControl.setMustRevalidate(true);
        
        return Response.ok(mapping)
                .cacheControl(cacheControl)
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .build();
    }
}

