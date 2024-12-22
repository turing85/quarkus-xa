package de.turing85.quarkus.xa;

import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GeneralExceptionMapper implements ExceptionMapper<Exception> {
  public static final String BODY = "{ \"message\": \"internal server error\"}";

  @Override
  public Response toResponse(Exception exception) {
    // @formatter:off
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .header(HttpHeaders.CONTENT_TYPE,
            "%s;charset=%s".formatted(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
        .entity(BODY)
        .build();
    // @formatter:on
  }
}
