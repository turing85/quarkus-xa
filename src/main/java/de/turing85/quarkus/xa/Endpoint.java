package de.turing85.quarkus.xa;

import java.net.URI;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.Session;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import lombok.RequiredArgsConstructor;

@Path(Endpoint.PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class Endpoint {
  public static final String PATH = "numbers";
  public static final String TOPIC_NUMBERS_CREATED = "numbers-created";

  private final EntityManager entityManager;
  private final ConnectionFactory connectionFactory;
  private final Finalizer finalizer;

  @POST
  @Transactional
  public Response createNumber(long number) {
    Number toCreate = Number.of(number);
    entityManager.persist(toCreate);
    try (JMSContext context = connectionFactory.createContext(Session.SESSION_TRANSACTED)) {
      context.createProducer().send(context.createTopic(TOPIC_NUMBERS_CREATED), number);
    }
    finalizer.end();
    // @formatter:off
    return Response
        .created(URI.create("%s/%d".formatted(PATH, number)))
        .entity(toCreate)
        .build();
    // @formatter:on
  }

  @GET
  @Path("{number}")
  public Response getNumber(@PathParam("number") long number) {
    // @formatter:off
    return Response.ok(
        entityManager
            .createQuery("SELECT number FROM Number number WHERE value = :value", Number.class)
            .setParameter("value", number)
            .getResultList())
        .build();
    // @formatter:on
  }
}
