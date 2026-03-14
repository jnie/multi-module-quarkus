package dk.jnie.example.advice.client;

import dk.jnie.example.advice.model.AdviceResponse;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/advice")
@RegisterRestClient(configKey = "advice-slip")
public interface AdviceSlipClient {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Uni<AdviceResponse> getRandomAdvice();
}