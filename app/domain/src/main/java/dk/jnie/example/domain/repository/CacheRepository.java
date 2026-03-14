package dk.jnie.example.domain.repository;

import io.smallrye.mutiny.Uni;

/**
 * Cache repository interface for storing and retrieving cached data.
 * 
 * <p>This interface defines the contract for a reactive cache implementation.
 * Implementations should use Mutiny's Uni for non-blocking operations.</p>
 */
public interface CacheRepository {

    /**
     * Retrieves a cached value by key.
     *
     * @param key the cache key
     * @return a Uni emitting the cached value, or null if not found
     */
    Uni<String> get(String key);

    /**
     * Stores a value in the cache.
     *
     * @param key the cache key
     * @param value the value to cache
     * @return a Uni emitting true if successful, false otherwise
     */
    Uni<Boolean> put(String key, String value);

    /**
     * Removes a value from the cache.
     *
     * @param key the cache key
     * @return a Uni emitting true if the value was evicted, false otherwise
     */
    Uni<Boolean> evict(String key);

    /**
     * Checks if a key exists in the cache.
     *
     * @param key the cache key
     * @return a Uni emitting true if the key exists, false otherwise
     */
    Uni<Boolean> exists(String key);
}