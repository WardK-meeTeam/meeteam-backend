package com.wardk.meeteam_backend.web.mainpage.dto.response;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecruitmentPositionResponse implements Serializable {

	private static final long serialVersionUID = 1L;

	private String jobFieldName; // 프론트엔드
	private String jobPositionName; // 웹프론트엔드
	private int currentCount; // 현재 인원
	private int recruitmentCount; // 모집 정원
	private boolean isClosed; // 마감 여부
	private List<String> techStacks; // ["React.js", "Typescript"]
}
