package dk.jnie.example.advice.mappers;

import dk.jnie.example.advice.model.AdviceResponse;
import dk.jnie.example.domain.model.MultiAggregate;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AdviceObjectMapper {

    public MultiAggregate toDomain(AdviceResponse response) {
        if (response == null || response.getSlip() == null) {
            return MultiAggregate.builder().answer("No advice available").build();
        }
        return MultiAggregate.builder()
                .answer(response.getSlip().getAdvice())
                .build();
    }
}