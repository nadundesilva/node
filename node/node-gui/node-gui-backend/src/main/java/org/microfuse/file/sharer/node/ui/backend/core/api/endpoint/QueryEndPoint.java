package org.microfuse.file.sharer.node.ui.backend.core.api.endpoint;

import com.google.gson.Gson;
import org.microfuse.file.sharer.node.core.resource.AggregatedResource;
import org.microfuse.file.sharer.node.core.utils.QueryManager;
import org.microfuse.file.sharer.node.ui.backend.commons.APIConstants;
import org.microfuse.file.sharer.node.ui.backend.commons.Status;
import org.microfuse.file.sharer.node.ui.backend.core.utils.FileSharerHolder;
import org.microfuse.file.sharer.node.ui.backend.core.utils.FileSharerMode;
import org.microfuse.file.sharer.node.ui.backend.core.utils.ResponseUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Querying related end point.
 */
@Path("/query")
public class QueryEndPoint {
    @POST
    @Path("/{queryString}")
    public Response runQuery(@PathParam("queryString") String queryString) {
        Map<String, Object> response;

        if (FileSharerHolder.getMode() == FileSharerMode.FILE_SHARER) {
            response = ResponseUtils.generateCustomResponse(Status.SUCCESS);

            QueryManager queryManager = FileSharerHolder.getFileSharer().getServiceHolder().getQueryManager();
            queryManager.query(queryString);
        } else {
            response = ResponseUtils.generateCustomResponse(Status.IN_TRACER_MODE);
        }

        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }

    @GET
    public Response getQueryResult() {
        Map<String, Object> response;

        if (FileSharerHolder.getMode() == FileSharerMode.FILE_SHARER) {
            response = ResponseUtils.generateCustomResponse(Status.SUCCESS);

            QueryManager queryManager = FileSharerHolder.getFileSharer().getServiceHolder().getQueryManager();
            Set<String> runningQueryStrings = queryManager.getRunningQueryStrings();
            response.put(APIConstants.DATA, runningQueryStrings);
        } else {
            response = ResponseUtils.generateCustomResponse(Status.IN_TRACER_MODE);
        }

        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/{queryString}")
    public Response getQueryResult(@PathParam("queryString") String queryString) {
        Map<String, Object> response;

        if (FileSharerHolder.getMode() == FileSharerMode.FILE_SHARER) {
            response = ResponseUtils.generateCustomResponse(Status.SUCCESS);

            QueryManager queryManager = FileSharerHolder.getFileSharer().getServiceHolder().getQueryManager();
            List<AggregatedResource> aggregatedResourceList = queryManager.getQueryResults(queryString);
            response.put(APIConstants.DATA, aggregatedResourceList);
        } else {
            response = ResponseUtils.generateCustomResponse(Status.IN_TRACER_MODE);
        }

        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }

    @DELETE
    public Response clearResults() {
        Map<String, Object> response;

        if (FileSharerHolder.getMode() == FileSharerMode.FILE_SHARER) {
            response = ResponseUtils.generateCustomResponse(Status.SUCCESS);

            FileSharerHolder.getFileSharer().getServiceHolder().getQueryManager().clearQueryResults();
        } else {
            response = ResponseUtils.generateCustomResponse(Status.IN_TRACER_MODE);
        }

        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }
}
