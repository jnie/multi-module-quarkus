package dk.jnie.example.advice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AdviceResponse(Slip slip) {
    public record Slip(
        @JsonProperty("slip_id") String slipId,
        String advice
    ) {}
}