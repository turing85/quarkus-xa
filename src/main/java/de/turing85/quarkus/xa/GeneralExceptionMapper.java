package de.turing85.quarkus.xa;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class GeneralExceptionMapper implements ExceptionMapper<Exception> {
  public static final String BODY = "{\"message\": \"internal server error\"}";

  @Override
  public Response toResponse(Exception exception) {
    // @formatter:off
    return Response
        .status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .entity(BODY)
        .build();
    // @formatter:on
  }
}
