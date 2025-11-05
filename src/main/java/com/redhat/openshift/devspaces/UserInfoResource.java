package com.redhat.openshift.devspaces;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Path("/api/user")
public class UserInfoResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserInfo(@Context HttpHeaders headers) {
        String user = headers.getHeaderString("X-Forwarded-User");
        String groups = headers.getHeaderString("X-Forwarded-Groups");

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode userInfo = mapper.createObjectNode();
        userInfo.put("user", user);
        userInfo.put("groups", groups);

        return Response.ok(userInfo).build();
    }
}