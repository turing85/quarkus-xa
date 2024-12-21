package de.turing85.quarkus.xa;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Finalizer {
  public void end() {
    // empty on purpose, will be mocked in tests to verify error behaviour
  }
}
