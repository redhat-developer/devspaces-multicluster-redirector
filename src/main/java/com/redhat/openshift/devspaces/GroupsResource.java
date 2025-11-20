package com.redhat.openshift.devspaces;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/api/groups")
public class GroupsResource {

    @Inject
    OpenShiftGroupService openShiftGroupService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllGroups() {
        List<String> groups = openShiftGroupService.getAllGroups();
        
        // Set cache control headers to prevent caching
        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        cacheControl.setNoStore(true);
        cacheControl.setMustRevalidate(true);
        
        return Response.ok(groups)
                .cacheControl(cacheControl)
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .build();
    }
}

