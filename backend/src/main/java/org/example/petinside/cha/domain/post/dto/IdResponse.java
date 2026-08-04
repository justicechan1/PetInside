package org.example.petinside.cha.domain.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IdResponse {
    private Long id;

    public static IdResponse from(Long id) {
        return new IdResponse(id);
    }
}