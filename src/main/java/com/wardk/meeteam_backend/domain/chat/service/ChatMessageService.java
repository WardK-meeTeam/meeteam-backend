package com.wardk.meeteam_backend.domain.chat.service;

import com.wardk.meeteam_backend.domain.chat.dto.ChatMessageDto;
import com.wardk.meeteam_backend.domain.chat.dto.ChatRequestDto;
import com.wardk.meeteam_backend.domain.chat.entity.*;
import com.wardk.meeteam_backend.domain.chat.repository.ChatMessageRepository;
import com.wardk.meeteam_backend.domain.chat.repository.ChatRoomMemberRepository;
import com.wardk.meeteam_backend.domain.chat.repository.ChatRoomRepository;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 채팅 메시지 관리를 담당하는 서비스 클래스입니다.
 *
 * @author MeeTeam Backend Team
 * @version 1.0
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MemberRepository memberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 채팅방 메시지를 페이징하여 조회합니다.
     * Service 단에서 Pageable을 적절히 튜닝하여 성능을 최적화합니다.
     *
     * @param roomId 채팅방 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기 (기본값: 20, 최대값: 100)
     * @param memberId 요청한 사용자 ID (권한 확인용)
     * @return 페이징된 채팅 메시지 DTO 목록
     */
    public Page<ChatMessageDto> getChatMessages(Long roomId, int page, int size, Long memberId) {
        log.info("Service 시작: ChatMessageService.getChatMessages - 파라미터: [roomId={}, page={}, size={}, memberId={}]",
                roomId, page, size, memberId);

        // 1. 채팅방 존재 및 권한 확인
        validateChatRoomAccess(roomId, memberId);

        // 2. Pageable 튜닝 - 성능 최적화를 위한 제한 설정
        Pageable optimizedPageable = createOptimizedPageable(page, size);

        // 3. 최적화된 쿼리로 메시지 조회
        Page<Object[]> messageData = chatMessageRepository.findChatMessageInfoByRoomId(roomId, optimizedPageable);

        // 4. Object[]를 DTO로 변환
        Page<ChatMessageDto> result = messageData.map(this::convertToDto);

        log.info("Service 완료: ChatMessageService.getChatMessages - 조회된 메시지 수: {}", result.getContent().size());
        return result;
    }

    /**
     * 커서 기반 페이징으로 메시지를 조회합니다.
     * 무한 스크롤에 최적화된 방식입니다.
     *
     * @param roomId 채팅방 ID
     * @param cursorId 커서 메시지 ID (null이면 최신 메시지부터)
     * @param size 조회할 메시지 수
     * @param memberId 요청한 사용자 ID
     * @return 메시지 DTO 목록
     */
    public List<ChatMessageDto> getChatMessagesByCursor(Long roomId, Long cursorId, int size, Long memberId) {
        log.info("Service 시작: ChatMessageService.getChatMessagesByCursor - 파라미터: [roomId={}, cursorId={}, size={}, memberId={}]",
                roomId, cursorId, size, memberId);

        validateChatRoomAccess(roomId, memberId);

        // 커서 기반 조회를 위한 Pageable 생성
        Pageable pageable = PageRequest.of(0, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "sentAt"));

        Page<Object[]> messageData;
        if (cursorId == null) {
            // 첫 번째 조회 - 최신 메시지부터
            messageData = chatMessageRepository.findChatMessageInfoByRoomId(roomId, pageable);
        } else {
            // 커서 이후 메시지 조회 (추가 구현 필요)
            messageData = chatMessageRepository.findChatMessageInfoByRoomId(roomId, pageable);
        }

        List<ChatMessageDto> result = messageData.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        log.info("Service 완료: ChatMessageService.getChatMessagesByCursor - 조회된 메시지 수: {}", result.size());
        return result;
    }

    /**
     * 읽지 않은 메시지 수를 조회합니다.
     *
     * @param roomId 채팅방 ID
     * @param memberId 사용자 ID
     * @return 읽지 않은 메시지 수
     */
    public long getUnreadMessageCount(Long roomId, Long memberId) {
        validateChatRoomAccess(roomId, memberId);

        // 사용자의 마지막 읽은 시간 조회
        ChatRoomMember roomMember = chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND));

        LocalDateTime lastReadAt = roomMember.getLastReadAt() != null ?
                roomMember.getLastReadAt() : roomMember.getJoinedAt();

        return chatMessageRepository.countUnreadMessages(roomId, lastReadAt, memberId);
    }

    /**
     * 성능 최적화를 위한 Pageable 생성
     * 페이지 크기 제한 및 정렬 조건 안전성 검증
     */
    private Pageable createOptimizedPageable(int page, int size) {
        // 페이지 크기 제한 (최대 100개)
        int optimizedSize = Math.min(size > 0 ? size : 20, 100);
        int optimizedPage = Math.max(page, 0);

        // 채팅 메시지에서 허용되는 정렬 컬럼만 허용
        Sort sort = createSafeSort();

        return PageRequest.of(optimizedPage, optimizedSize, sort);
    }

    /**
     * 안전한 정렬 조건 생성
     * 채팅 메시지에서 허용되는 컬럼과 방향만 허용
     */
    private Sort createSafeSort() {
        // 채팅에서는 항상 최신순이 기본이므로 sentAt DESC로 고정
        // 필요시 추가 정렬 옵션을 여기서 제어
        return Sort.by(Sort.Direction.DESC, "sentAt");
    }

    /**
     * 사용자 요청 Pageable을 안전하게 처리
     * 클라이언트에서 보낸 Sort 정보를 검증하고 안전한 정렬로 대체
     */
    private Pageable createSafePageable(Pageable userPageable) {
        // 사용자 페이징 정보에서 페이지와 크기만 추출
        int page = userPageable.getPageNumber();
        int size = userPageable.getPageSize();

        // 안전한 Pageable로 재생성 (Sort는 무시하고 안전한 Sort 적용)
        return createOptimizedPageable(page, size);
    }

    /**
     * 사용자 정의 정렬이 필요한 경우를 위한 고급 메서드
     * 허용된 컬럼과 방향만 적용
     */
    private Sort createValidatedSort(Sort userSort) {
        // 허용되는 정렬 컬럼 목록
        Set<String> allowedProperties = Set.of("sentAt", "id");

        // 허용되는 정렬 방향 (성능상 DESC만 허용)
        Set<Sort.Direction> allowedDirections = Set.of(Sort.Direction.DESC);

        if (userSort == null || userSort.isUnsorted()) {
            return Sort.by(Sort.Direction.DESC, "sentAt");
        }

        List<Sort.Order> validOrders = userSort.stream()
                .filter(order -> allowedProperties.contains(order.getProperty()))
                .filter(order -> allowedDirections.contains(order.getDirection()))
                .collect(Collectors.toList());

        if (validOrders.isEmpty()) {
            // 유효한 정렬이 없으면 기본 정렬 적용
            return Sort.by(Sort.Direction.DESC, "sentAt");
        }

        return Sort.by(validOrders);
    }

    /**
     * 채팅방 접근 권한 검증
     */
    private void validateChatRoomAccess(Long roomId, Long memberId) {
        // 채팅방 존재 확인
        if (!chatRoomRepository.existsById(roomId)) {
            throw new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        // 채팅방 멤버 권한 확인
        if (!chatRoomMemberRepository.existsByChatRoomIdAndMemberId(roomId, memberId)) {
            throw new CustomException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
    }

    /**
     * Object[] 데이터를 ChatMessageDto로 변환
     * 인덱스 순서: id, senderId, senderName, content, messageType, sentAt, isRead
     */
    private ChatMessageDto convertToDto(Object[] data) {
        return ChatMessageDto.builder()
                .id((Long) data[0])
                .senderId((Long) data[1])
                .senderName((String) data[2])
                .content((String) data[3])
                .messageType((MessageType) data[4])
                .createdAt((LocalDateTime) data[5])
                .isEdited((Boolean) data[6])
                .build();
    }

    /**
     * 메시지 전송 (TODO: 구현 예정)
     */
    @Transactional
    public ChatMessageDto sendMessage(ChatRequestDto requestDto, Long senderId) {
        // TODO: 구현 예정
        log.info("TODO: 메시지 전송 기능 구현 예정");
        throw new UnsupportedOperationException("메시지 전송 기능은 구현 예정입니다.");
    }

    /**
     * 메시지 수정 (TODO: 구현 예정)
     */
    @Transactional
    public ChatMessageDto updateMessage(Long messageId, String newContent, Long memberId) {
        // TODO: 구현 예정
        log.info("TODO: 메시지 수정 기능 구현 예정");
        throw new UnsupportedOperationException("메시지 수정 기능은 구현 예정입니다.");
    }

    /**
     * 메시지 삭제 (TODO: 구현 예정)
     */
    @Transactional
    public void deleteMessage(Long messageId, Long memberId) {
        // TODO: 구현 예정
        log.info("TODO: 메시지 삭제 기능 구현 예정");
        throw new UnsupportedOperationException("메시지 삭제 기능은 구현 예정입니다.");
    }

    /**
     * 읽음 상태 업데이트 (TODO: 구현 예정)
     */
    @Transactional
    public void markAsRead(Long roomId, Long memberId) {
        // TODO: 구현 예정
        log.info("TODO: 읽음 상태 업데이트 기능 구현 예정");
        throw new UnsupportedOperationException("읽음 상태 업데이트 기능은 구현 예정입니다.");
    }
}
