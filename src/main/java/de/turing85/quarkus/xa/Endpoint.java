package de.turing85.quarkus.xa;

import java.net.URI;

import jakarta.jms.XAConnectionFactory;
import jakarta.jms.XAJMSContext;
import jakarta.persistence.EntityManager;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
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

  private final TransactionManager transactionManager;
  private final EntityManager entityManager;
  private final XAConnectionFactory xaConnectionFactory;
  private final Finalizer finalizer;

  @POST
  @Transactional
  public Response createNumber(long number) throws SystemException, RollbackException {
    Number toCreate = Number.of(number);
    entityManager.persist(toCreate);
    Transaction transaction = transactionManager.getTransaction();
    XAJMSContext context = xaConnectionFactory.createXAContext();
    transaction.enlistResource(context.getXAResource());
    transaction.registerSynchronization(new Synchronization() {
      @Override
      public void beforeCompletion() {
        // nothing to do
      }

      @Override
      public void afterCompletion(int status) {
        context.close();
      }
    });
    context.createProducer().send(context.createTopic(TOPIC_NUMBERS_CREATED), number);
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
    return Response
        .ok(entityManager
            .createQuery("SELECT number FROM Number number WHERE value = :value", Number.class)
            .setParameter("value", number)
            .getResultList())
        .build();
    // @formatter:on
  }
}
