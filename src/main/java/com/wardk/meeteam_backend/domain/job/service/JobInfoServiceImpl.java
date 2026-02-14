package com.wardk.meeteam_backend.domain.job.service;

import com.wardk.meeteam_backend.domain.job.entity.JobField;
import com.wardk.meeteam_backend.domain.job.entity.JobPosition;
import com.wardk.meeteam_backend.domain.job.entity.TechStack;
import com.wardk.meeteam_backend.domain.job.repository.JobFieldRepository;
import com.wardk.meeteam_backend.domain.job.repository.JobPositionRepository;
import com.wardk.meeteam_backend.domain.job.repository.TechStackRepository;
import com.wardk.meeteam_backend.web.job.dto.response.JobFieldOptionResponse;
import com.wardk.meeteam_backend.web.job.dto.response.JobOptionResponse;
import com.wardk.meeteam_backend.web.job.dto.response.JobPositionOptionResponse;
import com.wardk.meeteam_backend.web.job.dto.response.TechStackOptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobInfoServiceImpl implements JobInfoService {

    private final JobFieldRepository jobFieldRepository;
    private final JobPositionRepository jobPositionRepository;
    private final TechStackRepository techStackRepository;

    @Override
    public JobOptionResponse getJobOptions() {
        List<JobField> fields = jobFieldRepository.findAllWithPositions();
        List<JobPosition> allPositions = jobPositionRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        List<TechStack> allTechStacks = techStackRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));

        List<JobFieldOptionResponse> fieldResponses = fields.stream()
                .map(this::toFieldResponse)
                .toList();

        List<JobPositionOptionResponse> positionResponses = allPositions.stream()
                .map(this::toPositionResponse)
                .toList();

        List<TechStackOptionResponse> techStackResponses = allTechStacks.stream()
                .map(this::toTechStackResponse)
                .toList();

        return JobOptionResponse.of(fieldResponses, positionResponses, techStackResponses);
    }

    private JobFieldOptionResponse toFieldResponse(JobField field) {
        List<JobPositionOptionResponse> positions = field.getJobPositions().stream()
                .sorted(Comparator.comparing(JobPosition::getId))
                .map(this::toPositionResponse)
                .toList();

        List<TechStackOptionResponse> techStacks = field.getJobFieldTechStacks().stream()
                .map(jfts -> toTechStackResponse(jfts.getTechStack()))
                .distinct()
                .sorted(Comparator.comparing(TechStackOptionResponse::id))
                .toList();

        return JobFieldOptionResponse.builder()
                .id(field.getId())
                .code(field.getCode())
                .name(field.getName())
                .positions(positions)
                .techStacks(techStacks)
                .build();
    }

    private JobPositionOptionResponse toPositionResponse(JobPosition position) {
        return JobPositionOptionResponse.builder()
                .id(position.getId())
                .fieldId(position.getJobField().getId())
                .code(position.getCode())
                .name(position.getName())
                .build();
    }

    private TechStackOptionResponse toTechStackResponse(TechStack techStack) {
        return TechStackOptionResponse.builder()
                .id(techStack.getId())
                .name(techStack.getName())
                .build();
    }
}
