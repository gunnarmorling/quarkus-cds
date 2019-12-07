package org.acme.quarkus.cds;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.enterprise.event.Observes;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.runtime.StartupEvent;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExampleResource {

    @GET
    public List<Todo> getAll() {
        System.out.println(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()));
        return Todo.findAll().list();
    }

    @POST
    @Transactional
    public Response create(Todo todo) {
        todo.persist();
        return Response.created(URI.create("/" + todo.id)).entity(todo).build();
    }

    void onStart(@Observes StartupEvent startup) {
        System.out.println(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()));
    }
}
