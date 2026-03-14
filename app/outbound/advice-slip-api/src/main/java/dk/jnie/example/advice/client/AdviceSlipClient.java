package dk.jnie.example.advice.client;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/advice")
@RegisterRestClient(configKey = "advice-slip")
public interface AdviceSlipClient {

    @GET
    Uni<String> getRandomAdvice();
}