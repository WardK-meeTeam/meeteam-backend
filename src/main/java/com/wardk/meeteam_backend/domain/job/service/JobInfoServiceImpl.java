package com.wardk.meeteam_backend.domain.job.service;

import com.wardk.meeteam_backend.domain.job.entity.JobField;
import com.wardk.meeteam_backend.domain.job.entity.JobFieldTechStack;
import com.wardk.meeteam_backend.domain.job.entity.JobPosition;
import com.wardk.meeteam_backend.domain.job.entity.TechStack;
import com.wardk.meeteam_backend.domain.job.repository.JobFieldRepository;
import com.wardk.meeteam_backend.domain.job.repository.JobFieldTechStackRepository;
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

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobInfoServiceImpl implements JobInfoService {

    private final JobFieldRepository jobFieldRepository;
    private final JobPositionRepository jobPositionRepository;
    private final TechStackRepository techStackRepository;
    private final JobFieldTechStackRepository jobFieldTechStackRepository;

    @Override
    public JobOptionResponse getJobOptions() {
        List<JobField> fields = jobFieldRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        List<JobPosition> positions = jobPositionRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        List<TechStack> techStacks = techStackRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        List<JobFieldTechStack> fieldTechStacks = jobFieldTechStackRepository.findAll();

        Map<Long, List<JobPositionOptionResponse>> positionsByFieldId = positions.stream()
                .map(this::toPositionResponse)
                .collect(Collectors.groupingBy(JobPositionOptionResponse::fieldId));

        Map<Long, TechStackOptionResponse> techStackById = techStacks.stream()
                .map(this::toTechStackResponse)
                .collect(Collectors.toMap(TechStackOptionResponse::id, Function.identity()));

        Map<Long, List<TechStackOptionResponse>> techStacksByFieldId = fieldTechStacks.stream()
                .collect(Collectors.groupingBy(
                        mapping -> mapping.getJobField().getId(),
                        Collectors.mapping(
                                mapping -> techStackById.get(mapping.getTechStack().getId()),
                                Collectors.collectingAndThen(Collectors.toMap(
                                        TechStackOptionResponse::id,
                                        Function.identity(),
                                        (left, right) -> left
                                ), map -> map.values().stream()
                                        .sorted((a, b) -> a.id().compareTo(b.id()))
                                        .toList()
                                )
                        )
                ));

        List<JobFieldOptionResponse> fieldResponses = fields.stream()
                .map(field -> JobFieldOptionResponse.builder()
                        .id(field.getId())
                        .code(field.getCode())
                        .name(field.getName())
                        .positions(positionsByFieldId.getOrDefault(field.getId(), List.of()))
                        .techStacks(techStacksByFieldId.getOrDefault(field.getId(), List.of()))
                        .build())
                .toList();

        List<JobPositionOptionResponse> positionResponses = positions.stream()
                .map(this::toPositionResponse)
                .toList();

        List<TechStackOptionResponse> techStackResponses = techStacks.stream()
                .map(this::toTechStackResponse)
                .toList();

        return JobOptionResponse.builder()
                .fields(fieldResponses)
                .positions(positionResponses)
                .techStacks(techStackResponses)
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
