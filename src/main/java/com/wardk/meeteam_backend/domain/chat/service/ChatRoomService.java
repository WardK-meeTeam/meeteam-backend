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
 * 채팅방 관리를 담당하는 서비스 클래스입니다.
 *
 * <p>이 서비스는 다음과 같은 기능을 제공합니다:</p>
 * <ul>
 *   <li>프로젝트 기본 채팅방 자동 생성</li>
 *   <li>개인 채팅방(1:1) 생성 및 조회</li>
 *   <li>주제별 채팅방 생성</li>
 *   <li>채팅방 멤버 관리 (초대, 퇴장)</li>
 *   <li>사용자별 채팅방 목록 조회</li>
 *   <li>시스템 메시지 자동 전송</li>
 * </ul>
 *
 * <p>모든 채팅방 관련 비즈니스 로직은 트랜잭션으로 관리되며,
 * WebSocket을 통한 실시간 알림도 함께 처리됩니다.</p>
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

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 프로젝트 생성 시 자동으로 기본 채팅방을 생성합니다.
     *
     * <p>프로젝트가 생성되면 팀 전체가 소통할 수 있는 기본 채팅방이 자동으로 생성됩니다.
     * 채팅방 이름은 "{프로젝트명} 팀 채팅" 형태로 설정됩니다.</p>
     *
     * @param projectId 프로젝트 ID
     * @param creatorId 프로젝트 생성자 ID (채팅방 생성자가 됨)
     * @return 생성된 채팅방 정보
     * @throws CustomException 프로젝트를 찾을 수 없거나 이미 기본 채팅방이 존재하는 경우
     */
    @Transactional
    public ChatRoomDto createProjectDefaultChatRoom(Long projectId, Long creatorId) {
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

        // 생성자를 멤버로 추가
        addMemberToChatRoom(chatRoom.getId(), creatorId);

        return convertToChatRoomDto(chatRoom);
    }

    /**
     * 두 사용자 간의 개인 채팅방을 생성하거나 기존 채팅방을 조회합니다.
     *
     * <p>이미 두 사용자 간의 개인 채팅방이 존재하면 기존 채팅방을 반환하고,
     * 없다면 새로운 개인 채팅방을 생성합니다.</p>
     *
     * @param userId1 첫 번째 사용자 ID
     * @param userId2 두 번째 사용자 ID
     * @return 개인 채팅방 정보
     * @throws CustomException 자기 자신과 채팅하려고 시도하거나 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public ChatRoomDto getOrCreatePrivateChatRoom(Long userId1, Long userId2) {
        if (userId1.equals(userId2)) {
            throw new CustomException(ErrorCode.CANNOT_CHAT_WITH_YOURSELF);
        }

        // 기존 개인 채팅방 검색
        return chatRoomRepository.findPrivateChatRoom(ChatRoomType.PRIVATE, userId1, userId2)
                .map(this::convertToChatRoomDto)
                .orElseGet(() -> createNewPrivateChatRoom(userId1, userId2));
    }

    /**
     * 새로운 개인 채팅방을 생성합니다.
     *
     * <p>두 사용자를 자동으로 멤버로 추가하고, 채팅방 이름은
     * "{사용자1}, {사용자2}" 형태로 설정됩니다.</p>
     *
     * @param userId1 첫 번째 사용자 ID
     * @param userId2 두 번째 사용자 ID
     * @return 생성된 개인 채팅방 정보
     * @throws CustomException 사용자를 찾을 수 없는 경우
     */
    @Transactional
    protected ChatRoomDto createNewPrivateChatRoom(Long userId1, Long userId2) {
        Member member1 = memberRepository.findById(userId1)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        Member member2 = memberRepository.findById(userId2)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        ChatRoom chatRoom = ChatRoom.builder()
                .name(member1.getNickname() + ", " + member2.getNickname())
                .type(ChatRoomType.PRIVATE)
                .creatorId(userId1)
                .build();

        chatRoom = chatRoomRepository.save(chatRoom);

        // 두 사용자를 멤버로 추가
        addMemberToChatRoom(chatRoom.getId(), userId1);
        addMemberToChatRoom(chatRoom.getId(), userId2);

        return convertToChatRoomDto(chatRoom);
    }

    /**
     * 프로젝트 내에서 주제별 채팅방을 생성합니다.
     *
     * <p>특정 주제나 기능에 대해 논의하기 위한 별도의 채팅방을 생성합니다.
     * 예: 'UI/UX 논의', '백엔드 개발', '배포 관련' 등</p>
     *
     * @param request 채팅방 생성 요청 정보 (이름, 설명, 프로젝트 ID, 초기 멤버 등)
     * @param creatorId 채팅방 생성자 ID
     * @return 생성된 주제별 채팅방 정보
     * @throws CustomException 프로젝트 ID가 없거나 프로젝트를 찾을 수 없는 경우
     */
    @Transactional
    public ChatRoomDto createTopicChatRoom(ChatRequestDto.CreateChatRoom request, Long creatorId) {
        if (request.getProjectId() == null) {
            throw new CustomException(ErrorCode.PROJECT_ID_REQUIRED);
        }

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        ChatRoom chatRoom = ChatRoom.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(ChatRoomType.TOPIC)
                .project(project)
                .creatorId(creatorId)
                .build();

        chatRoom = chatRoomRepository.save(chatRoom);

        // 생성자를 멤버로 추가
        addMemberToChatRoom(chatRoom.getId(), creatorId);

        // 초기 멤버들 추가
        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            request.getMemberIds().forEach(memberId -> {
                if (!memberId.equals(creatorId)) {
                    addMemberToChatRoom(chatRoom.getId(), memberId);
                }
            });
        }

        return convertToChatRoomDto(chatRoom);
    }

    /**
     * 채팅방에 멤버 추가
     */
    @Transactional
    public void addMemberToChatRoom(Long chatRoomId, Long memberId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 이미 참여 중인지 확인
        if (chatRoomMemberRepository.findByChatRoomIdAndMemberIdAndIsActiveTrue(chatRoomId, memberId).isPresent()) {
            return; // 이미 참여 중이면 무시
        }

        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .member(member)
                .lastReadTime(LocalDateTime.now())
                .build();

        chatRoomMemberRepository.save(chatRoomMember);

        // 입장 메시지 전송
        sendSystemMessage(chatRoomId, member.getNickname() + "님이 채팅방에 입장했습니다.");
    }

    /**
     * 사용자의 채팅방 목록 조회
     */
    public List<ChatRoomDto> getUserChatRooms(Long memberId) {
        return chatRoomRepository.findByMemberIdOrderByLastMessageTimeDesc(memberId)
                .stream()
                .map(this::convertToChatRoomDto)
                .collect(Collectors.toList());
    }

    /**
     * 채팅방 상세 정보 조회
     */
    public ChatRoomDto getChatRoomDetail(Long chatRoomId, Long memberId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 멤버 권한 확인
        chatRoomMemberRepository.findByChatRoomIdAndMemberIdAndIsActiveTrue(chatRoomId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));

        return convertToChatRoomDto(chatRoom);
    }

    /**
     * 시스템 메시지 전송
     */
    @Transactional
    protected void sendSystemMessage(Long chatRoomId, String content) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        ChatMessage systemMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .senderRole(SenderRole.SYSTEM)
                .content(content)
                .messageType(MessageType.SYSTEM)
                .build();

        chatMessageRepository.save(systemMessage);

        // WebSocket으로 실시간 전송
        ChatMessageDto messageDto = ChatMessageDto.builder()
                .id(systemMessage.getId())
                .chatRoomId(chatRoomId)
                .content(content)
                .messageType(MessageType.SYSTEM)
                .type(ChatMessageDto.ChatMessageType.CHAT)
                .createdAt(systemMessage.getCreatedAt())
                .build();

        messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId, messageDto);
    }

    /**
     * ChatRoom 엔티티를 ChatRoomDto로 변환합니다.
     *
     * <p>채팅방의 기본 정보와 함께 활성 멤버 수, 마지막 메시지 등의
     * 추가 정보를 포함하여 DTO로 변환합니다.</p>
     *
     * @param chatRoom 변환할 ChatRoom 엔티티
     * @return 변환된 ChatRoomDto
     */
    private ChatRoomDto convertToChatRoomDto(ChatRoom chatRoom) {
        List<ChatRoomMember> activeMembers = chatRoomMemberRepository.findByChatRoomIdAndIsActiveTrue(chatRoom.getId());
        ChatMessage lastMessage = chatMessageRepository.findLastMessageByChatRoomId(chatRoom.getId());

        return ChatRoomDto.builder()
                .id(chatRoom.getId())
                .name(chatRoom.getName())
                .description(chatRoom.getDescription())
                .type(chatRoom.getType())
                .projectId(chatRoom.getProject() != null ? chatRoom.getProject().getId() : null)
                .projectName(chatRoom.getProject() != null ? chatRoom.getProject().getTitle() : null)
                .creatorId(chatRoom.getCreatorId())
                .isActive(chatRoom.getIsActive())
                .lastMessageTime(chatRoom.getLastMessageTime())
                .memberCount(activeMembers.size())
                .lastMessage(lastMessage != null ? lastMessage.getContent() : null)
                .createdAt(chatRoom.getCreatedAt())
                .members(activeMembers.stream()
                        .map(member -> ChatRoomDto.ChatRoomMemberDto.builder()
                                .memberId(member.getMember().getId())
                                .memberName(member.getMember().getNickname())
                                .profileImage(member.getMember().getProfileImgSrc())
                                .lastReadTime(member.getLastReadTime())
                                .isActive(member.getIsActive())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
