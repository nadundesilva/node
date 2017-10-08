package org.microfuse.file.sharer.node.server.api;

import com.google.gson.Gson;
import org.microfuse.file.sharer.node.server.utils.ResponseUtils;
import org.microfuse.file.sharer.node.server.utils.Status;

import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Querying related end point.
 */
@Path("/query")
public class QueryEndPoint {
    @GET
    @Path("/{fileName}")
    public Response trainModel(@PathParam("fileName") String name) {
        Map<String, Object> response = ResponseUtils.generateCustomResponse(Status.SUCCESS);
        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }
}
