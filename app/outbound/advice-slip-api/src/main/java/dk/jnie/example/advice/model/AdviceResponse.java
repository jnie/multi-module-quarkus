package dk.jnie.example.advice.model;

public record AdviceResponse(Slip slip) {
    public record Slip(Long sl_id, String advice) {}
}