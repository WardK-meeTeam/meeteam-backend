package com.wardk.meeteam_backend.web.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MessageSendRequest(
    @Schema(description = "메시지", example = "리뷰 잘 봤어. 너가 고치라고 한 그 부분을 고치는 이유가 뭐야?")
    String text
) {}
