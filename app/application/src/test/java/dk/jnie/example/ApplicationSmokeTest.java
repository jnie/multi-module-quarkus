package dk.jnie.example;

import dk.jnie.example.advice.AdviceApiImpl;
import dk.jnie.example.advice.client.AdviceSlipClient;
import dk.jnie.example.domain.outbound.AdviceApi;
import dk.jnie.example.domain.repository.CacheRepository;
import dk.jnie.example.domain.services.OurService;
import dk.jnie.example.repository.CacheRepositoryImpl;
import dk.jnie.example.rest.controllers.MainController;
import dk.jnie.example.rest.mappers.RestMapper;
import dk.jnie.example.services.OurServiceImpl;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ApplicationSmokeTest {

    @Inject
    OurService ourService;

    @Inject
    AdviceApi adviceApi;

    @Inject
    CacheRepository cacheRepository;

    @Inject
    MainController mainController;

    @Inject
    RestMapper restMapper;

    @Test
    @DisplayName("Application context should load successfully")
    void applicationContextLoads() {
        assertNotNull(ourService, "OurService bean should be injected");
        assertNotNull(adviceApi, "AdviceApi bean should be injected");
        assertNotNull(cacheRepository, "CacheRepository bean should be injected");
        assertNotNull(mainController, "MainController bean should be injected");
        assertNotNull(restMapper, "RestMapper bean should be injected");
    }

    @Test
    @DisplayName("Service layer should be properly wired")
    void serviceLayerIsProperlyWired() {
        assertTrue(ourService instanceof OurServiceImpl, 
                "OurService should be implemented by OurServiceImpl");
    }

    @Test
    @DisplayName("Repository layer should be properly wired")
    void repositoryLayerIsProperlyWired() {
        assertTrue(cacheRepository instanceof CacheRepositoryImpl,
                "CacheRepository should be implemented by CacheRepositoryImpl");
    }

    @Test
    @DisplayName("Outbound adapter should be properly wired")
    void outboundAdapterIsProperlyWired() {
        assertTrue(adviceApi instanceof AdviceApiImpl,
                "AdviceApi should be implemented by AdviceApiImpl");
    }
}