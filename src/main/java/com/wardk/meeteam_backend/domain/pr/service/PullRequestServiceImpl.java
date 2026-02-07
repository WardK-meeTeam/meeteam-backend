package com.wardk.meeteam_backend.domain.pr.service;

import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;
import com.wardk.meeteam_backend.domain.pr.repository.PullRequestRepository;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.web.pr.dto.response.PullRequestResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PullRequestServiceImpl implements PullRequestService {

    private final PullRequestRepository pullRequestRepository;

    @Override
    public PullRequestResponse getPullRequest(String repoFullName, Integer prNumber) {
        PullRequest pr = pullRequestRepository.findWithFiles(repoFullName, prNumber)
                .orElseThrow(() -> new CustomException(ErrorCode.PR_NOT_FOUND));

        return PullRequestResponse.create(pr);
    }

    @Override
    public List<PullRequestResponse> getAllPullRequests(Long projectId) {

        List<PullRequest> prs = pullRequestRepository.findAllByProjectIdWithFiles(projectId);

        return PullRequestResponse.createList(prs);
    }

    @Override
    public List<PullRequestResponse> getAllPullRequestsInRepo(String repoFullName) {
        List<PullRequest> prs = pullRequestRepository.findAllByRepoFullNameWithFiles(repoFullName);
        return PullRequestResponse.createList(prs);
    }
}
