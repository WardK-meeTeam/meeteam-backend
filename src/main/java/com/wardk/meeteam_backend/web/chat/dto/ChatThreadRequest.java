package com.wardk.meeteam_backend.web.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatThreadRequest {
  @Schema(hidden = true)
  private Long memberId;

  @Schema(description = "페이지 번호 (0부터 시작)", example = "0", required = true)
  private int pageNumber;

  @Schema(description = "페이지 크기", example = "20", required = true)
  private int pageSize;
}