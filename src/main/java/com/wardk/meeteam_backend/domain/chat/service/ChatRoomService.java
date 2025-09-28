package com.wardk.meeteam_backend.domain.chat.service;

import com.wardk.meeteam_backend.domain.chat.dto.ChatMessageDto;
import com.wardk.meeteam_backend.domain.chat.dto.ChatRequestDto;
import com.wardk.meeteam_backend.domain.chat.dto.ChatRoomDto;
import com.wardk.meeteam_backend.domain.chat.entity.*;
import com.wardk.meeteam_backend.domain.chat.repository.ChatMessageRepository;
import com.wardk.meeteam_backend.domain.chat.repository.ChatRoomMemberRepository;
import com.wardk.meeteam_backend.domain.chat.repository.ChatRoomRepository;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 채팅방 관리를 담당하는 서비스 클래스입니다. (TODO: 구현 예정)
 *
 * <p>이 서비스는 다음과 같은 기능을 제공할 예정입니다:</p>
 * <ul>
 *   <li>프로젝트 기본 채팅방 자동 생성</li>
 *   <li>개인 채팅방(1:1) 생성 및 조회</li>
 *   <li>주제별 채팅방 생성</li>
 *   <li>채팅방 멤버 관리 (초대, 퇴장)</li>
 *   <li>사용자별 채팅방 목록 조회</li>
 *   <li>시스템 메시지 자동 전송</li>
 * </ul>
 *
 * @author MeeTeam Backend Team
 * @version 1.0
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChatRoomService {

    // TODO: 실제 레포지토리들 구현 필요
    // private final ChatRoomRepository chatRoomRepository;
    // private final ChatRoomMemberRepository chatRoomMemberRepository;
    // private final ChatMessageRepository chatMessageRepository;
    // private final MemberRepository memberRepository;
    // private final ProjectRepository projectRepository;
    // private final SimpMessagingTemplate messagingTemplate;

    /**
     * 프로젝트 생성 시 자동으로 기본 채팅방을 생성합니다.
     *
     * TODO: 구현 필요
     * - 프로젝트 기본 채팅방 생성 로직
     * - 중복 생성 방지
     * - 생성자 자동 멤버 추가
     */
    @Transactional
    public ChatRoomDto createProjectDefaultChatRoom(Long projectId, Long creatorId) {
        log.info("프로젝트 기본 채팅방 생성 시도 - 프로젝트 ID: {}, 생성자 ID: {}", projectId, creatorId);

        // TODO: 실제 채팅방 생성 로직 구현
        /*
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        // 이미 기본 채팅방이 있는지 확인
        if (chatRoomRepository.findByProjectIdAndType(projectId, ChatRoomType.PROJECT).isPresent()) {
            throw new CustomException(ErrorCode.CHAT_ROOM_ALREADY_EXISTS);
        }

        ChatRoom chatRoom = ChatRoom.builder()
                .name(project.getTitle() + " 팀 채팅")
                .description("프로젝트 기본 채팅방")
                .type(ChatRoomType.PROJECT)
                .project(project)
                .creatorId(creatorId)
                .build();

        chatRoom = chatRoomRepository.save(chatRoom);
        */

        log.warn("ChatRoomService.createProjectDefaultChatRoom() - 미구현 메서드 호출됨");
        throw new UnsupportedOperationException("ChatRoomService.createProjectDefaultChatRoom() 구현 필요");
    }

    /**
     * 두 사용자 간의 개인 채팅방을 생성하거나 기존 채팅방을 조회합니다.
     *
     * TODO: 구현 필요
     * - 기존 개인 채팅방 검색 로직
     * - 새 개인 채팅방 생성 로직
     * - 중복 생성 방지
     */
    @Transactional
    public ChatRoomDto getOrCreatePrivateChatRoom(Long userId1, Long userId2) {
        log.info("개인 채팅방 생성/조회 시도 - 사용자 1: {}, 사용자 2: {}", userId1, userId2);

        if (userId1.equals(userId2)) {
            throw new CustomException(ErrorCode.CANNOT_CHAT_WITH_YOURSELF);
        }

        log.warn("ChatRoomService.getOrCreatePrivateChatRoom() - 미구현 메서드 호출됨");
        throw new UnsupportedOperationException("ChatRoomService.getOrCreatePrivateChatRoom() 구현 필요");
    }

    /**
     * 새로운 개인 채팅방을 생성합니다.
     *
     * TODO: 구현 필요
     * - 개인 채팅방 생성 로직
     * - 양쪽 사용자 자동 멤버 추가
     */
    @Transactional
    protected ChatRoomDto createNewPrivateChatRoom(Long userId1, Long userId2) {
        log.info("새 개인 채팅방 생성 - 사용자 1: {}, 사용자 2: {}", userId1, userId2);

        log.warn("ChatRoomService.createNewPrivateChatRoom() - 미구현 메서드 호출됨");
        throw new UnsupportedOperationException("ChatRoomService.createNewPrivateChatRoom() 구현 필요");
    }

    /**
     * 프로젝트 내에서 주제별 채팅방을 생성합니다.
     *
     * TODO: 구현 필요
     * - 주제별 채팅방 생성 로직
     * - 초기 멤버 추가
     * - 시스템 메시지 전송
     */
    @Transactional
    public ChatRoomDto createTopicChatRoom(ChatRequestDto.CreateChatRoom request, Long creatorId) {
        log.info("주제별 채팅방 생성 시도 - 생성자 ID: {}, 채팅방명: {}", creatorId, request.getName());

        if (request.getProjectId() == null) {
            throw new CustomException(ErrorCode.PROJECT_ID_REQUIRED);
        }

        log.warn("ChatRoomService.createTopicChatRoom() - 미구현 메서드 호출됨");
        throw new UnsupportedOperationException("ChatRoomService.createTopicChatRoom() 구현 필요");
    }

    /**
     * 채팅방에 멤버 추가
     *
     * TODO: 구현 필요
     * - 멤버 추가 로직
     * - 중복 멤버 확인
     * - 입장 메시지 전송
     */
    @Transactional
    public void addMemberToChatRoom(Long chatRoomId, Long memberId) {
        log.info("채팅방 멤버 추가 - 채팅방 ID: {}, 멤버 ID: {}", chatRoomId, memberId);

        log.warn("ChatRoomService.addMemberToChatRoom() - 미구현 메서드 호출됨");
        throw new UnsupportedOperationException("ChatRoomService.addMemberToChatRoom() 구현 필요");
    }

    /**
     * 사용자의 채팅방 목록 조회
     *
     * TODO: 구현 필요
     * - 사용자별 채팅방 목록 조회
     * - 마지막 메시지 시간 순 정렬
     * - DTO 변환
     */
    public List<ChatRoomDto> getUserChatRooms(Long memberId) {
        log.info("사용자 채팅방 목록 조회 - 사용자 ID: {}", memberId);

        log.warn("ChatRoomService.getUserChatRooms() - 미구현 메서드 호출됨");
        throw new UnsupportedOperationException("ChatRoomService.getUserChatRooms() 구현 필요");
    }

    /**
     * 채팅방 상세 정보 조회
     *
     * TODO: 구현 필요
     * - 채팅방 정보 조회
     * - 멤버 권한 확인
     * - DTO 변환
     */
    public ChatRoomDto getChatRoomDetail(Long chatRoomId, Long memberId) {
        log.info("채팅방 상세 정보 조회 - 채팅방 ID: {}, 사용자 ID: {}", chatRoomId, memberId);

        log.warn("ChatRoomService.getChatRoomDetail() - 미구현 메서드 호출됨");
        throw new UnsupportedOperationException("ChatRoomService.getChatRoomDetail() 구현 필요");
    }

    /**
     * 시스템 메시지 전송 (TODO: 구현 필요)
     */
    @Transactional
    protected void sendSystemMessage(Long chatRoomId, String content) {
        log.info("시스템 메시지 전송 - 채팅방 ID: {}, 내용: {}", chatRoomId, content);

        // TODO: 시스템 메시지 전송 로직 구현
        /*
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        ChatMessage systemMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .senderRole(SenderRole.SYSTEM)
                .content(content)
                .messageType(MessageType.SYSTEM)
                .build();

        chatMessageRepository.save(systemMessage);
        // WebSocket 브로드캐스트
        */

        log.warn("ChatRoomService.sendSystemMessage() - 미구현 메서드 호출됨");
    }

    /**
     * ChatRoom 엔티티를 ChatRoomDto로 변환합니다. (TODO: 구현 필요)
     */
    private ChatRoomDto convertToChatRoomDto(ChatRoom chatRoom) {
        // TODO: 엔티티→DTO 변환 로직 구현
        return ChatRoomDto.builder().build();
    }
}
