package com.yfmf.footlog.domain.member.dto;

import lombok.Getter;

@Getter
public class MemberDeleteResponseDTO {
    private String message;
    private Long memberId;

    public MemberDeleteResponseDTO(String message, Long memberId) {
        this.message = message;
        this.memberId = memberId;
    }
}
