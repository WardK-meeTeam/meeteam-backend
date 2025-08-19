package com.wardk.meeteam_backend.domain.notification.service;


import com.wardk.meeteam_backend.domain.chat.entity.ChatThread;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;

import java.util.List;

public interface NotificationService {
  void sendPrArrived(List<Member> targets, PullRequest pr, ChatThread thread);
}
