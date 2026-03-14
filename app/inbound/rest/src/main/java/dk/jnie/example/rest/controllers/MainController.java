package dk.jnie.example.rest.controllers;

import dk.jnie.example.domain.model.DomainRequest;
import dk.jnie.example.domain.services.OurService;
import dk.jnie.example.rest.mappers.RestMapper;
import dk.jnie.example.rest.model.RequestDto;
import dk.jnie.example.rest.model.ResponseDto;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Path("/api/v1/advice")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Advice API", description = "Get inspirational advice")
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    private final OurService ourService;
    private final RestMapper restMapper;

    public MainController(OurService ourService, RestMapper restMapper) {
        this.ourService = ourService;
        this.restMapper = restMapper;
    }

    @POST
    @Operation(summary = "Get advice", description = "Returns a random piece of advice")
    @APIResponse(
            responseCode = "200",
            description = "Success",
            content = @Content(schema = @Schema(implementation = ResponseDto.class))
    )
    @APIResponse(responseCode = "400", description = "Invalid input")
    public Uni<Response> getAdvice(
            @Valid @RequestBody(
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = RequestDto.class),
                            examples = @ExampleObject(value = "{\"question\": \"give me advice\"}")
                    )
            ) RequestDto request) {

        log.debug("Received request: {}", request.question());

        DomainRequest domainRequest = restMapper.toDomain(request);

        return ourService.getAnAdvice(domainRequest)
                .onItem().transform(restMapper::toDto)
                .onItem().transform(dto -> Response.ok(dto).build())
                .onFailure().recoverWithItem(error -> {
                    log.error("Error processing request", error);
                    return Response.serverError()
                            .entity("{\"error\": \"Unable to retrieve advice\"}")
                            .build();
                });
    }
}