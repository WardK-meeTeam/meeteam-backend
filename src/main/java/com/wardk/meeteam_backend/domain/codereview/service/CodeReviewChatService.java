package com.wardk.meeteam_backend.domain.codereview.service;

import com.wardk.meeteam_backend.domain.chat.dto.ChatMessageDto;
import com.wardk.meeteam_backend.domain.chat.entity.*;
import com.wardk.meeteam_backend.domain.chat.repository.ChatMessageRepository;
import com.wardk.meeteam_backend.domain.chat.repository.ChatRoomRepository;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;
import com.wardk.meeteam_backend.domain.pr.repository.PullRequestRepository;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.web.chat.dto.MessageSendRequest;
import com.wardk.meeteam_backend.web.codereview.dto.CodeReviewChatRoomDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 코드리뷰 전용 채팅 서비스
 * 기존 채팅 시스템을 활용하여 PR 기반 코드리뷰 채팅 기능 제공
 *
 * @author MeeTeam Backend Team
 * @version 1.0
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CodeReviewChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;
    private final PullRequestRepository pullRequestRepository;
    private final CodeReviewChatBroadcastService broadcastService;


    /**
     * 채팅방 ID로 직접 실시간 메시지를 전송합니다. (권장)
     * 프론트엔드에서 이미 chatRoomId를 알고 있을 때 사용
     */
    @Transactional
    public ChatMessageDto sendRealtimeMessageByChatRoomId(Long chatRoomId, Long senderId, MessageSendRequest request) {
        // 채팅방 존재 확인
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 권한 확인 (채팅방 멤버인지 확인)
        validateChatRoomAccess(chatRoomId, senderId);

        // 메시지 생성 및 저장
        ChatMessageDto messageDto = createChatRoomMessage(chatRoomId, senderId, request.text());

        // 실시간 브로드캐스트
        Long prId = extractPrIdFromChatRoom(chatRoom);
        broadcastService.broadcastToCodeReviewRoom(prId, messageDto);

        return messageDto;
    }

    /**
     * PR 리뷰 시작 메시지를 채팅방에 추가합니다.
     */
    @Transactional
    public void addReviewStartMessage(Long chatRoomId, Integer prNumber, int totalFiles) {
        try {
            ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

            String startMessage = String.format("🚀 PR #%d 리뷰를 시작합니다. (총 %d개 파일)", prNumber, totalFiles);

            ChatMessage reviewStartMessage = ChatMessage.builder()
                    .chatRoom(chatRoom)
                    .sender(null) // 시스템 메시지
                    .content(startMessage)
                    .messageType(MessageType.SYSTEM)
                    .sentAt(LocalDateTime.now())
                    .isRead(false)
                    .build();

            ChatMessage savedMessage = chatMessageRepository.save(reviewStartMessage);

            // 채팅방의 마지막 메시지 정보 업데이트
            chatRoom.updateLastMessage(reviewStartMessage);
            chatRoomRepository.save(chatRoom);

            // 실시간 브로드캐스트
            ChatMessageDto messageDto = convertToMessageDto(savedMessage);
            Long prId = extractPrIdFromChatRoom(chatRoom);
            broadcastService.broadcastToCodeReviewRoom(prId, messageDto);

            log.info("리뷰 시작 메시지 추가 및 브로드캐스트 완료: 채팅방={}, PR={}", chatRoomId, prNumber);

        } catch (Exception e) {
            log.error("리뷰 시작 메시지 추가 실패: 채팅방={}, PR={}", chatRoomId, prNumber, e);
        }
    }

    /**
     * 파일 리뷰 결과를 채팅방에 메시지로 추가합니다.
     */
    @Transactional
    public void addFileReviewMessage(Long chatRoomId, String fileName, String reviewContent) {
        try {
            ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

            // 시스템 메시지로 파일 리뷰 결과 추가
            ChatMessage reviewMessage = ChatMessage.builder()
                    .chatRoom(chatRoom)
                    .sender(null) // 시스템 메시지는 sender가 null
                    .content(formatFileReviewMessage(fileName, reviewContent))
                    .messageType(MessageType.SYSTEM)
                    .sentAt(LocalDateTime.now())
                    .isRead(false)
                    .build();

            ChatMessage savedMessage = chatMessageRepository.save(reviewMessage);

            // 채팅방의 마지막 메시지 정보 업데이트
            chatRoom.updateLastMessage(reviewMessage);
            chatRoomRepository.save(chatRoom);

            // 실시간 브로드캐스트
            ChatMessageDto messageDto = convertToMessageDto(savedMessage);
            Long prId = extractPrIdFromChatRoom(chatRoom);
            broadcastService.broadcastToCodeReviewRoom(prId, messageDto);

            log.info("파일 리뷰 메시지 추가 및 브로드캐스트 완료: 채팅방={}, 파일={}", chatRoomId, fileName);

        } catch (Exception e) {
            log.error("파일 리뷰 메시지 추가 실패: 채팅방={}, 파일={}", chatRoomId, fileName, e);
            // 메시지 추가 실패해도 리뷰 프로세스는 계속 진행
        }
    }

    /**
     * PR 리뷰 완료 메시지를 채팅방에 추가합니다.
     */
    @Transactional
    public void addReviewCompleteMessage(Long chatRoomId, Integer prNumber, long successCount, long failedCount) {
        try {
            ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

            String completeMessage = String.format(
                    "✅ PR #%d 리뷰가 완료되었습니다!\n📊 성공: %d개, 실패: %d개",
                    prNumber, successCount, failedCount);

            ChatMessage reviewCompleteMessage = ChatMessage.builder()
                    .chatRoom(chatRoom)
                    .sender(null) // 시스템 메시지
                    .content(completeMessage)
                    .messageType(MessageType.SYSTEM)
                    .sentAt(LocalDateTime.now())
                    .isRead(false)
                    .build();

            ChatMessage savedMessage = chatMessageRepository.save(reviewCompleteMessage);

            // 채팅방의 마지막 메시지 정보 업데이트
            chatRoom.updateLastMessage(reviewCompleteMessage);
            chatRoomRepository.save(chatRoom);

            // 실시간 브로드캐스트
            ChatMessageDto messageDto = convertToMessageDto(savedMessage);
            Long prId = extractPrIdFromChatRoom(chatRoom);
            broadcastService.broadcastToCodeReviewRoom(prId, messageDto);

            log.info("리뷰 완료 메시지 추가 및 브로드캐스트 완료: 채팅방={}, PR={}", chatRoomId, prNumber);

        } catch (Exception e) {
            log.error("리뷰 완료 메시지 추가 실패: 채팅방={}, PR={}", chatRoomId, prNumber, e);
        }
    }

    /**
     * 파일 리뷰 메시지 포맷팅
     */
    private String formatFileReviewMessage(String fileName, String reviewContent) {
        StringBuilder message = new StringBuilder();
        message.append("📄 **").append(fileName).append("** 리뷰 완료\n\n");

        // 리뷰 내용이 너무 길면 요약
        if (reviewContent.length() > 1000) {
            message.append(reviewContent, 0, 997).append("...");
        } else {
            message.append(reviewContent);
        }

        return message.toString();
    }

    /**
     * 새로운 코드리뷰 채팅방 생성
     */
    @Transactional
    protected CodeReviewChatRoomDto createNewCodeReviewChatRoom(PullRequest pullRequest, Long creatorId) {
        Member creator = memberRepository.findById(creatorId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        ChatRoom chatRoom = ChatRoom.builder()
                .name("PR #" + pullRequest.getPrNumber() + ": " + pullRequest.getTitle())
                .description("코드리뷰 채팅방 - " + pullRequest.getTitle())
                .type(ChatRoomType.PR_REVIEW)
                .creatorId(creatorId)
                .isActive(true)
                .build();

        chatRoom = chatRoomRepository.save(chatRoom);

        log.info("코드리뷰 채팅방 생성 완료 - PR ID: {}, 채팅방 ID: {}", pullRequest.getId(), chatRoom.getId());

        return convertToChatRoomDto(chatRoom, pullRequest);
    }

    /**
     * 채팅방 이름에서 PR ID 추출 (임시 방법)
     */
    private Long extractPrIdFromChatRoom(ChatRoom chatRoom) {
        try {
            String name = chatRoom.getName();
            if (name.contains("PR #")) {
                String prNumberStr = name.substring(name.indexOf("PR #") + 4);
                if (prNumberStr.contains(":")) {
                    prNumberStr = prNumberStr.substring(0, prNumberStr.indexOf(":"));
                }
                return Long.parseLong(prNumberStr.trim());
            }
        } catch (Exception e) {
            log.warn("채팅방에서 PR ID 추출 실패: {}", chatRoom.getName(), e);
        }
        return 1L; // 기본값
    }

    private void validateUserAccess(PullRequest pullRequest, Long userId) {
        // TODO: 실제 프로젝트 멤버 권한 확인 로직 구현
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * 채팅방 멤버 권한 확인
     */
    private void validateChatRoomAccess(Long chatRoomId, Long userId) {
        // TODO: ChatRoomMember 테이블에서 해당 사용자가 채팅방 멤버인지 확인
        // 현재는 단순히 사용자 존재 여부만 확인
        memberRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Transactional
    protected ChatMessageDto createChatRoomMessage(Long chatRoomId, Long senderId, String content) {
        return createChatRoomMessage(chatRoomId, senderId, content, MessageType.TEXT);
    }

    @Transactional
    protected ChatMessageDto createChatRoomMessage(Long chatRoomId, Long senderId, String content, MessageType messageType) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        Member sender = null;
        if (senderId != null) {
            sender = memberRepository.findById(senderId)
                    .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        }

        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content(content)
                .messageType(messageType)
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .build();

        message = chatMessageRepository.save(message);

        return convertToMessageDto(message);
    }

    // ============================================
    // DTO Conversion Methods
    // ============================================

    private CodeReviewChatRoomDto convertToChatRoomDto(ChatRoom chatRoom, PullRequest pullRequest) {
        return CodeReviewChatRoomDto.builder()
                .id(chatRoom.getId())
                .name(chatRoom.getName())
                .description(chatRoom.getDescription())
                .prId(pullRequest.getId())
                .prTitle(pullRequest.getTitle())
                .prNumber(pullRequest.getPrNumber())
                .projectId(pullRequest.getProjectRepo().getProject().getId())
                .projectName(pullRequest.getProjectRepo().getProject().getName())
                .creatorId(chatRoom.getCreatorId())
                .createdAt(chatRoom.getCreatedAt())
                .lastMessageTime(chatRoom.getLastMessageAt())
                .build();
    }

    private ChatMessageDto convertToMessageDto(ChatMessage message) {
        return ChatMessageDto.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoom().getId())
                .senderId(message.getSender() != null ? message.getSender().getId() : null)
                .senderName(message.getSender() != null ? message.getSender().getRealName() : "시스템")
                .senderProfileImage(message.getSender() != null ? message.getSender().getStoreFileName() : null)
                .content(message.getContent())
                .messageType(message.getMessageType())
                .createdAt(message.getCreatedAt())
                .isEdited(false)
                .isDeleted(false)
                .build();
    }

    /**
     * 사용자가 속한 코드리뷰 채팅방 목록을 조회합니다.
     *
     * @param memberId 사용자 ID
     * @return 사용자가 속한 채팅방 목록
     */
    public List<CodeReviewChatRoomDto> getChatRoomsByMemberId(Long memberId) {
        log.info("사용자 채팅방 목록 조회 시작 - 사용자 ID: {}", memberId);

        // 사용자 존재 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 최적화된 쿼리로 필요한 컬럼만 조회
        List<ChatRoom> chatRoomInfo = chatRoomRepository.findByMemberId(memberId);

        // Object[] 배열을 DTO로 변환
        List<CodeReviewChatRoomDto> chatRoomDtos = chatRoomInfo.stream()
                .map(this::convertToChatRoomDto)
                .filter(Objects::nonNull) // null 값 필터링
                .collect(Collectors.toList());

        log.info("사용자 채팅방 목록 조회 완료 - 사용자 ID: {}, 채팅방 개수: {}", memberId, chatRoomDtos.size());

        return chatRoomDtos;
    }

    /**
     * Object[] 배열을 CodeReviewChatRoomDto로 변환 (최적화된 방식)
     * <p>
     * 쿼리 결과 순서: id, name, description, lastMessageAt, lastMessageContent,
     * isActive, createdAt, updatedAt, prReviewJobId
     */
    private CodeReviewChatRoomDto convertObjectArrayToChatRoomDto(Object[] row) {
        try {
            if (row == null || row.length < 9) {
                log.warn("잘못된 채팅방 데이터: {}", Arrays.toString(row));
                return null;
            }

            return CodeReviewChatRoomDto.builder()
                    .id((Long) row[0])
                    .name((String) row[1])
                    .description((String) row[2])
                    .lastMessageTime((LocalDateTime) row[3])
                    .lastMessage((String) row[4])
                    .createdAt((LocalDateTime) row[6])
                    .lastMessageTime((LocalDateTime) row[7])
                    .build();

        } catch (Exception e) {
            log.error("Object[] 배열을 DTO로 변환 실패: {}", Arrays.toString(row), e);
            return null;
        }
    }

    /**
     * 기존 방식 유지 (호환성을 위해) - 엔티티 전체 조회
     * 상세 정보가 필요하거나 연관 엔티티 정보가 필요할 때 사용
     */
    public List<CodeReviewChatRoomDto> getChatRoomsByMemberIdWithDetails(Long memberId) {
        log.info("사용자 채팅방 목록 조회 시작 (상세 정보 포함) - 사용자 ID: {}", memberId);

        // 사용자 존재 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 사용자가 속한 채팅방 목록 조회 (기존 방식)
        List<ChatRoom> chatRooms = chatRoomRepository.findChatRoomsByMemberId(memberId);

        // DTO 변환
        List<CodeReviewChatRoomDto> chatRoomDtos = chatRooms.stream()
                .map(this::convertToChatRoomDto)
                .collect(Collectors.toList());

        log.info("사용자 채팅방 목록 조회 완료 (상세 정보 포함) - 사용자 ID: {}, 채팅방 개수: {}", memberId, chatRoomDtos.size());

        return chatRoomDtos;
    }

    /**
     * ChatRoom 엔티티를 CodeReviewChatRoomDto로 변환
     */
    private CodeReviewChatRoomDto convertToChatRoomDto(ChatRoom chatRoom) {
        // PR 정보 추출
        PullRequest pullRequest = chatRoom.getPrReviewJob() != null ?
                chatRoom.getPrReviewJob().getPullRequest() : null;

        return CodeReviewChatRoomDto.builder()
                .id(chatRoom.getId())
                .name(chatRoom.getName())
                .description(chatRoom.getDescription())
                .prNumber(pullRequest != null ? pullRequest.getPrNumber() : null)
                .lastMessage(chatRoom.getLastMessageContent())
                .createdAt(chatRoom.getCreatedAt())
                .lastMessageTime(chatRoom.getLastMessageAt())
                .build();
    }

    // ============================================
    // 채팅 메시지 조회 메서드
    // ============================================

    /**
     * 채팅방 ID로 직접 메시지 히스토리를 조회합니다. (최적화된 버전)
     * 필요한 컬럼만 조회하여 성능을 개선합니다.
     */
    public Page<ChatMessageDto> getChatHistoryByChatRoomId(Long chatRoomId, Pageable pageable) {
        // 채팅방 존재 확인
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 최적화된 쿼리로 필요한 컬럼만 조회
        Page<Object[]> messageInfoPage = chatMessageRepository.findChatMessageInfoByRoomId(chatRoomId, pageable);

        // Object[] 배열을 DTO로 변환
        List<ChatMessageDto> messagesDtos = messageInfoPage.getContent().stream()
                .map(this::convertObjectArrayToMessageDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new PageImpl<>(messagesDtos, pageable, messageInfoPage.getTotalElements());
    }

    /**
     * 기본 메시지 정보만 조회 (가장 빠른 버전)
     * 간단한 메시지 목록이 필요할 때 사용
     */
    public Page<ChatMessageDto> getBasicChatHistory(Long chatRoomId, Pageable pageable) {
        // 채팅방 존재 확인
        chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 기본 정보만 조회
        Page<Object[]> basicMessagePage = chatMessageRepository.findBasicMessagesByRoomId(chatRoomId, pageable);

        // Object[] 배열을 DTO로 변환 (기본 정보만)
        List<ChatMessageDto> messagesDtos = basicMessagePage.getContent().stream()
                .map(this::convertBasicObjectArrayToMessageDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new PageImpl<>(messagesDtos, pageable, basicMessagePage.getTotalElements());
    }

    /**
     * 실시간 채팅용 최신 메시지 조회 (특정 시간 이후)
     */
    public List<ChatMessageDto> getRecentMessages(Long chatRoomId, LocalDateTime afterTime, int limit) {
        // 채팅방 존재 확인
        chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 최신 메시지 조회
        List<Object[]> recentMessages = chatMessageRepository.findRecentMessagesByRoomId(
                chatRoomId, afterTime, limit);

        return recentMessages.stream()
                .map(this::convertObjectArrayToMessageDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Object[] 배열을 ChatMessageDto로 변환 (전체 정보)
     *
     * 배열 순서: id, senderId, senderName, content, messageType, sentAt, isRead
     */
    private ChatMessageDto convertObjectArrayToMessageDto(Object[] row) {
        try {
            if (row == null || row.length < 7) {
                log.warn("잘못된 메시지 데이터: {}", Arrays.toString(row));
                return null;
            }

            return ChatMessageDto.builder()
                    .id((Long) row[0])
                    .chatRoomId(null) // 이미 알고 있는 정보이므로 생략 가능
                    .senderId((Long) row[1])
                    .senderName((String) row[2])
                    .senderProfileImage(null) // 필요시 추가 쿼리로 조회
                    .content((String) row[3])
                    .messageType((MessageType) row[4])
                    .createdAt((LocalDateTime) row[5])
                    .isEdited(false) // 기본값
                    .isDeleted(false) // 기본값
                    .build();

        } catch (Exception e) {
            log.error("Object[] 배열을 ChatMessageDto로 변환 실패: {}", Arrays.toString(row), e);
            return null;
        }
    }

    /**
     * Object[] 배열을 ChatMessageDto로 변환 (기본 정보만)
     *
     * 배열 순서: id, content, sentAt
     */
    private ChatMessageDto convertBasicObjectArrayToMessageDto(Object[] row) {
        try {
            if (row == null || row.length < 3) {
                log.warn("잘못된 기본 메시지 데이터: {}", Arrays.toString(row));
                return null;
            }

            return ChatMessageDto.builder()
                    .id((Long) row[0])
                    .content((String) row[1])
                    .createdAt((LocalDateTime) row[2])
                    .messageType(MessageType.TEXT) // 기본값
                    .isEdited(false)
                    .isDeleted(false)
                    .build();

        } catch (Exception e) {
            log.error("기본 Object[] 배열을 ChatMessageDto로 변환 실패: {}", Arrays.toString(row), e);
            return null;
        }
    }
}
