package dk.jnie.example.advice;

import dk.jnie.example.advice.client.AdviceSlipClient;
import dk.jnie.example.advice.mappers.AdviceObjectMapper;
import dk.jnie.example.domain.model.MultiAggregate;
import dk.jnie.example.domain.outbound.AdviceApi;
import dk.jnie.example.domain.repository.CacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AdviceApiImpl implements AdviceApi {

    private static final Logger log = LoggerFactory.getLogger(AdviceApiImpl.class);
    private static final String CACHE_KEY = "advice:random";

    private final AdviceObjectMapper mapper;
    private final AdviceSlipClient adviceClient;
    private final CacheRepository cacheRepository;
    private final boolean cacheEnabled;

    @Inject
    public AdviceApiImpl(
            AdviceObjectMapper mapper,
            @RestClient AdviceSlipClient adviceClient,
            CacheRepository cacheRepository,
            @ConfigProperty(name = "mma.cache.enabled", defaultValue = "false") boolean cacheEnabled) {
        this.mapper = mapper;
        this.adviceClient = adviceClient;
        this.cacheRepository = cacheRepository;
        this.cacheEnabled = cacheEnabled;
    }

    @Override
    public Uni<MultiAggregate> getRandomAdvice() {
        if (cacheEnabled) {
            return getFromCache()
                    .onItem().ifNull().switchTo(fetchAndCache());
        }
        return fetchFromApi();
    }

    private Uni<MultiAggregate> getFromCache() {
        return cacheRepository.get(CACHE_KEY)
                .onItem().transform(cached -> {
                    if (cached != null) {
                        log.info("Retrieved advice from cache");
                        return MultiAggregate.builder().answer(cached).build();
                    }
                    return null;
                });
    }

    private Uni<MultiAggregate> fetchAndCache() {
        return fetchFromApi()
                .onItem().transformToUni(aggregate ->
                        cacheRepository.put(CACHE_KEY, aggregate.getAnswer())
                                .onItem().transform(ignored -> aggregate)
                );
    }

    private Uni<MultiAggregate> fetchFromApi() {
        log.info("Calling the advice API");
        return adviceClient.getRandomAdvice()
                .onItem().transform(mapper::toDomain)
                .onFailure().invoke(e -> log.error("Failed to call advice API", e));
    }
}