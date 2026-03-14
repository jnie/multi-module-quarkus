package dk.jnie.example.rest.mappers;

import dk.jnie.example.domain.model.DomainRequest;
import dk.jnie.example.domain.model.DomainResponse;
import dk.jnie.example.rest.model.RequestDto;
import dk.jnie.example.rest.model.ResponseDto;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RestMapper {

    public DomainRequest toDomain(RequestDto dto) {
        if (dto == null) {
            return null;
        }
        return DomainRequest.builder()
                .question(dto.getQuestion())
                .build();
    }

    public ResponseDto toDto(DomainResponse response) {
        if (response == null) {
            return null;
        }
        ResponseDto dto = new ResponseDto();
        dto.setAnswer(response.getAnswer());
        return dto;
    }
}