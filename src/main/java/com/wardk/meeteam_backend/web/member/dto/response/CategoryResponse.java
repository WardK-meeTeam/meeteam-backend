package com.wardk.meeteam_backend.web.member.dto.response;


import com.wardk.meeteam_backend.domain.job.JobField;
import com.wardk.meeteam_backend.domain.job.JobPosition;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryResponse {


    private JobField jobField;

    private JobPosition jobPosition;

}
