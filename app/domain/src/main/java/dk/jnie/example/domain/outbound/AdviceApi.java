package dk.jnie.example.domain.outbound;

import dk.jnie.example.domain.model.MultiAggregate;
import io.smallrye.mutiny.Uni;

/**
 * AdviceApi is an interface that defines a contract for fetching random advice.
 * 
 * <p>It returns a {@link Uni} that emits a {@link MultiAggregate}, which contains 
 * the aggregated response data. This interface is used in a reactive programming 
 * context with Mutiny.</p>
 * 
 * <p>Implementations should handle external API calls to retrieve advice data
 * and map responses to domain models.</p>
 */
public interface AdviceApi {

    /**
     * Fetches a random piece of advice.
     *
     * <p>This method is designed to be non-blocking and returns a {@link Uni},
     * which represents a single asynchronous computation. The Uni will emit a
     * {@link MultiAggregate} object that encapsulates the random advice data.</p>
     *
     * @return a {@link Uni} emitting the {@link MultiAggregate} object containing the advice data.
     *         The Uni completes when the advice is successfully fetched or with an error if the fetch fails.
     */
    Uni<MultiAggregate> getRandomAdvice();
}