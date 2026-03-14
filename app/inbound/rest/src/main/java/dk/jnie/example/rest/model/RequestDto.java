package dk.jnie.example.rest.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RequestDto {
    @NotBlank(message = "Question is required")
    private String question;
}