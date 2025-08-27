package com.wardk.meeteam_backend.web.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageSendRequest(

    @NotBlank(message = "메시지는 비어 있을 수 없습니다.")
    @Size(max = 1000, message = "메시지는 최대 100자까지 허용됩니다.")
    @Schema(description = "메시지", example = "리뷰 잘 봤어. 너가 고치라고 한 그 부분을 고치는 이유가 뭐야?")
    String text
) {}
