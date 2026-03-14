package dk.jnie.example.services;

import dk.jnie.example.domain.outbound.AdviceApi;
import dk.jnie.example.domain.services.OurService;
import dk.jnie.example.domain.model.DomainRequest;
import dk.jnie.example.domain.model.DomainResponse;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service implementation for advice retrieval.
 * 
 * <p>This class uses CDI annotations (@ApplicationScoped, @Inject) for dependency 
 * injection. While some Clean Architecture purists argue for framework-agnostic 
 * service layers, this is a pragmatic choice:</p>
 * 
 * <ul>
 *   <li><b>Domain remains pure:</b> The domain module has zero framework dependencies.
 *       Clean Architecture's primary concern is protecting the domain core.</li>
 *   <li><b>CDI is the bean mechanism:</b> @ApplicationScoped is a bean lifecycle annotation,
 *       not a framework coupling concern. It tells the container "this is a singleton bean"
 *       - similar to how @Service works in Spring. The service is still just implementing
 *       an interface contract defined in domain.</li>
 *   <li><b>Separation of concerns preserved:</b> Business logic remains in the service
 *       implementation, not scattered across producer classes in the application layer.</li>
 *   <li><b>Testability unaffected:</b> Can still unit test with plain constructors.
 *       CDI only matters at runtime integration.</li>
 * </ul>
 * 
 * <p>The key Clean Architecture principle is keeping the domain layer framework-agnostic.
 * Service layer annotations are acceptable because:</p>
 * 
 * <ol>
 *   <li>Service defines business logic orchestration (implementation detail)</li>
 *   <li>Domain defines contracts and business rules (stable core)</li>
 *   <li>Inbound/Outbound adapters depend on domain interfaces, not service</li>
 * </ol>
 * 
 * @see OurService
 */
@ApplicationScoped
public class OurServiceImpl implements OurService {

    private static final Logger log = LoggerFactory.getLogger(OurServiceImpl.class);
    
    private final AdviceApi adviceAPI;
    
    @Inject
    public OurServiceImpl(AdviceApi adviceAPI) {
        this.adviceAPI = adviceAPI;
    }
    
    @Override
    public Uni<DomainResponse> getAnAdvice(DomainRequest domainRequest) {
        log.debug("Requesting advice for: {}", domainRequest.getQuestion());
        return adviceAPI.getRandomAdvice()
                .map(advice -> DomainResponse.builder()
                        .answer(advice.getAnswer())
                        .build());
    }
}