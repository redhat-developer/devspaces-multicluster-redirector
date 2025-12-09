package com.redhat.openshift.devspaces;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class NotFoundRedirectFilterTest {

    @Test
    public void testNotFoundRedirect() {
        given()
          .redirects().follow(false) // Disable automatic redirect following to check the 302 response
          .when().get("/non-existent-path")
          .then()
             .statusCode(302)
             .header("Location", is("/"));
    }

    @Test
    public void testNotFoundRedirectWithHash() {
        given()
                .redirects().follow(false) // Disable automatic redirect following to check the 302 response
                .when().get("/non-existent-path#hash")
                .then()
                .statusCode(302)
                .header("Location", is("/"));
    }
}
