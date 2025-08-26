package com.wardk.meeteam_backend.web.mainpage.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SliceResponse<T> { // 무한 스크롤용 페이징 응답 래퍼
    private List<T> content;
    private Boolean hasNext;
    private Integer currentPage;
    private Integer size;

    public static <T> SliceResponse<T> of(List<T> contents, Boolean hasNext, Integer currentPage) {
        return SliceResponse.<T>builder()
                .content(contents)
                .hasNext(hasNext)
                .currentPage(currentPage)
                .size(contents.size())
                .build();
    }
}