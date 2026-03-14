package dk.jnie.example.domain.services;

import dk.jnie.example.domain.model.DomainRequest;
import dk.jnie.example.domain.model.DomainResponse;
import io.smallrye.mutiny.Uni;

/**
 * Service interface for advice retrieval.
 * 
 * <p>This interface defines the business logic contract for getting advice.
 * Implementations should be provided in the service module.</p>
 * 
 * @see DomainRequest
 * @see DomainResponse
 */
public interface OurService {

    /**
     * Retrieves advice based on the provided request.
     *
     * @param domainRequest the request containing the question
     * @return a Uni emitting the domain response containing the advice
     */
    Uni<DomainResponse> getAnAdvice(DomainRequest domainRequest);
}