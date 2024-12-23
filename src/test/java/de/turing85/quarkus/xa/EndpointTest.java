package de.turing85.quarkus.xa;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.google.common.truth.Truth;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class EndpointTest {
  private static final String MEDIA_TYPE_APPLICATION_JSON_UTF_8 =
      "%s;charset=%s".formatted(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);
  private static final Random RANDOM = new Random();

  @Inject
  @SuppressWarnings("CdiInjectionPointsInspection")
  ConnectionFactory connectionFactory;

  @TestHTTPEndpoint(Endpoint.class)
  @TestHTTPResource
  URL url;

  @Inject
  EntityManager entityManager;

  @InjectMock
  Finalizer finalizer;

  private JMSContext context;
  private JMSConsumer consumer;

  @BeforeEach
  void setup() {
    context = connectionFactory.createContext();
    consumer =
        context.createSharedConsumer(context.createTopic(Endpoint.TOPIC_NUMBERS_CREATED), "test");
  }

  @AfterEach
  void tearDown() {
    Optional.ofNullable(consumer).ifPresent(JMSConsumer::close);
    consumer = null;
    Optional.ofNullable(context).ifPresent(JMSContext::close);
    context = null;
  }

  private Optional<Message> getMessage() {
    return Optional.ofNullable(consumer.receive(Duration.ofSeconds(5).toMillis()));
  }

  private List<Number> getEntriesWithValue(long numberToSend) {
    // @formatter:off
    return entityManager
        .createQuery("SELECT number FROM Number number WHERE value = :value", Number.class)
        .setParameter("value", numberToSend)
        .getResultList();
    // @formatter:on
  }

  @Nested
  @TestHTTPEndpoint(Endpoint.class)
  class HappyPath {
    @Test
    void whenAllIsGood_thenShouldCreateAndSend() throws JMSException {
      // given
      long valueToSend = RANDOM.nextLong();
      // @formatter:off
      Number expectedNumber = RestAssured
          .given()
              .contentType(MediaType.APPLICATION_JSON)
              .body(valueToSend)

      // when
          .when().post()

      // then
          .then()
              .statusCode(is(Response.Status.CREATED.getStatusCode()))
              .contentType(is(MEDIA_TYPE_APPLICATION_JSON_UTF_8))
              .header(HttpHeaders.LOCATION, is("%s/%d".formatted(url, valueToSend)))
              .body("value", is(valueToSend))
              .extract().body().as(Number.class);
      // @formatter:on

      Mockito.verify(finalizer, Mockito.times(1)).end();

      Optional<Message> maybeMessage = getMessage();
      Truth.assertThat(maybeMessage).isPresent();
      Truth.assertThat(maybeMessage.get().getBody(Long.class)).isEqualTo(valueToSend);

      List<Number> results = getEntriesWithValue(expectedNumber.getValue());
      Truth.assertThat(results).hasSize(1);
      Truth.assertThat(results.getFirst().getValue()).isEqualTo(expectedNumber.getValue());

      // @formatter:off
      List<Number> actualNumbers = RestAssured
          .when().get(Long.toString(valueToSend))
          .then()
              .statusCode(is(Response.Status.OK.getStatusCode()))
              .contentType(is(MEDIA_TYPE_APPLICATION_JSON_UTF_8))
          .extract().body().as(new TypeRef<>() {});
      // @formatter:on
      Truth.assertThat(actualNumbers).hasSize(1);
      Truth.assertThat(actualNumbers.getFirst().getId()).isEqualTo(expectedNumber.getId());
      Truth.assertThat(actualNumbers.getFirst().getValue()).isEqualTo(expectedNumber.getValue());
    }
  }

  @Nested
  @TestHTTPEndpoint(Endpoint.class)
  class RainyPath {
    @Test
    void whenFinalizerFails_thenShouldNotCreateAndSend() {
      // given
      long valueToSend = RANDOM.nextLong();
      Mockito.doThrow(new RuntimeException("Exception to test transaction")).when(finalizer).end();
      // @formatter:off
      RestAssured
          .given()
              .contentType(MediaType.APPLICATION_JSON)
              .body(valueToSend)

      // when
          .when().post()

      // then
          .then()
              .statusCode(is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()))
              .contentType(is(MEDIA_TYPE_APPLICATION_JSON_UTF_8))
              .body(is(GeneralExceptionMapper.BODY));
      // @formatter:on

      Mockito.verify(finalizer, Mockito.times(1)).end();

      Truth.assertThat(getMessage()).isEmpty();

      Truth.assertThat(getEntriesWithValue(valueToSend)).isEmpty();

      // @formatter:off
      List<Number> actualNumbers = RestAssured
          .when().get(Long.toString(valueToSend))
          .then()
              .statusCode(is(Response.Status.OK.getStatusCode()))
              .contentType(is(MEDIA_TYPE_APPLICATION_JSON_UTF_8))
          .extract().body().as(new TypeRef<>() {});
      // @formatter:on
      Truth.assertThat(actualNumbers).hasSize(0);
    }
  }
}
