package org.acme.quarkus.cds;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ExampleResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
          .when().get("/api")
          .then()
             .statusCode(200)
             .body(is("[{\"id\":1,\"title\":\"Be Awesome\"},{\"id\":2,\"title\":\"Learn Quarkus\"}]"));
    }
}
