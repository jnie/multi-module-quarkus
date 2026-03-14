package dk.jnie.example.repository;

import dk.jnie.example.domain.repository.CacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class CacheRepositoryImpl implements CacheRepository {

    private static final Logger log = LoggerFactory.getLogger(CacheRepositoryImpl.class);

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public CacheRepositoryImpl() {
        log.info("In-memory cache repository initialized");
    }

    @Override
    public Uni<String> get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry != null) {
            log.debug("Cache hit for key: {}", key);
            return Uni.createFrom().item(entry.value);
        }
        log.debug("Cache miss for key: {}", key);
        return Uni.createFrom().nullItem();
    }

    @Override
    public Uni<Boolean> put(String key, String value) {
        cache.put(key, new CacheEntry(value, Instant.now()));
        log.debug("Cached value for key: {}", key);
        return Uni.createFrom().item(true);
    }

    @Override
    public Uni<Boolean> evict(String key) {
        cache.remove(key);
        log.debug("Evicted cache for key: {}", key);
        return Uni.createFrom().item(true);
    }

    @Override
    public Uni<Boolean> exists(String key) {
        boolean exists = cache.containsKey(key);
        log.debug("Key {} exists: {}", key, exists);
        return Uni.createFrom().item(exists);
    }

    private record CacheEntry(String value, Instant createdAt) {}
}