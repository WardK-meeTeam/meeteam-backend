package com.wardk.meeteam_backend.web.member.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupedSkillResponse {

	private String jobFieldName; // 프론트엔드
	private String jobPositionName; // 웹프론트엔드
	private List<String> techStacks; // ["React.js", "Next.js"]
}
