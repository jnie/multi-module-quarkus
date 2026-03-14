package dk.jnie.example.advice.model;

import lombok.Data;

@Data
public class AdviceResponse {
    private Slip slip;

    @Data
    public static class Slip {
        private Long sl_id;
        private String advice;
    }
}