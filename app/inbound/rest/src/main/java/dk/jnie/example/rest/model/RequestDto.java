package dk.jnie.example.rest.model;

import jakarta.validation.constraints.NotBlank;

public record RequestDto(
    @NotBlank(message = "Question is required")
    String question
) {}