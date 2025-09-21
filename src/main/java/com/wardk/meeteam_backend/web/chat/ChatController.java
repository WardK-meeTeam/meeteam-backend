package com.wardk.meeteam_backend.web.chat;

import com.wardk.meeteam_backend.domain.chat.dto.ChatMessageDto;
import com.wardk.meeteam_backend.domain.chat.dto.ChatRequestDto;
import com.wardk.meeteam_backend.domain.chat.dto.ChatRoomDto;
import com.wardk.meeteam_backend.domain.chat.service.ChatMessageService;
import com.wardk.meeteam_backend.domain.chat.service.ChatRoomService;
import com.wardk.meeteam_backend.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 채팅 관련 REST API를 제공하는 컨트롤러입니다.
 *
 * <p>이 컨트롤러는 WebSocket 실시간 통신과 함께 사용되는 HTTP 기반 API를 제공합니다:</p>
 * <ul>
 *   <li>채팅방 목록 조회 및 관리</li>
 *   <li>채팅방 생성 (개인, 주제별)</li>
 *   <li>채팅 메시지 히스토리 조회 (페이징)</li>
 *   <li>멤버 초대 및 관리</li>
 *   <li>읽음 상태 관리</li>
 *   <li>메시지 수정 및 삭제</li>
 * </ul>
 *
 * <p>모든 API는 JWT 기반 인증이 필요하며, Spring Security를 통해 사용자 인증을 처리합니다.
 * API 응답은 CommonResponse 형태로 통일되어 있으며, Swagger를 통해 문서화됩니다.</p>
 *
 * @author MeeTeam Backend Team
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat", description = "채팅 관련 API")
public class ChatController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    /**
     * 현재 사용자가 참여한 모든 채팅방 목록을 조회합니다.
     *
     * <p>마지막 메시지 시간 순으로 정렬되어 반환되며, 각 채팅방의 정보에는
     * 읽지 않은 메시지 수, 마지막 메시지 내용, 참여자 수 등이 포함됩니다.</p>
     *
     * @param memberId 현재 로그인한 사용자 ID (Spring Security에서 자동 주입)
     * @return 사용자가 참여한 채팅방 목록
     */
    @Operation(summary = "사용자의 채팅방 목록 조회", description = "현재 사용자가 참여한 모든 채팅방 목록을 조회합니다.")
    @GetMapping("/rooms")
    public CommonResponse<List<ChatRoomDto>> getUserChatRooms(
            @AuthenticationPrincipal Long memberId) {

        List<ChatRoomDto> chatRooms = chatRoomService.getUserChatRooms(memberId);
        return CommonResponse.success("채팅방 목록 조회 성공", chatRooms);
    }

    /**
     * 특정 채팅방의 상세 정보를 조회합니다.
     *
     * <p>채팅방의 기본 정보, 참여자 목록, 설정 등을 포함한 상세 정보를 반환합니다.
     * 해당 채팅방의 멤버만 조회할 수 있습니다.</p>
     *
     * @param chatRoomId 조회할 채팅방 ID
     * @param memberId 현재 로그인한 사용자 ID (Spring Security에서 자동 주입)
     * @return 채팅방 상세 정보
     * @throws CustomException 채팅방을 찾을 수 없거나 접근 권한이 없는 경우
     */
    @Operation(summary = "채팅방 상세 정보 조회", description = "특정 채팅방의 상세 정보를 조회합니다.")
    @GetMapping("/rooms/{chatRoomId}")
    public CommonResponse<ChatRoomDto> getChatRoomDetail(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal Long memberId) {

        ChatRoomDto chatRoom = chatRoomService.getChatRoomDetail(chatRoomId, memberId);
        return CommonResponse.success("채팅방 상세 정보 조회 성공", chatRoom);
    }

    /**
     * 두 사용자 간의 개인 채팅방을 생성하거나 기존 채팅방을 조회합니다.
     *
     * <p>이미 두 사용자 간의 개인 채팅방이 존재하면 기존 채팅방을 반환하고,
     * 없다면 새로운 개인 채팅방을 생성합니다. 주로 프로젝트 지원 전 팀장과의
     * 개인적인 질문을 위해 사용됩니다.</p>
     *
     * @param targetMemberId 채팅을 시작할 상대방 사용자 ID
     * @param memberId 현재 로그인한 사용자 ID (Spring Security에서 자동 주입)
     * @return 개인 채팅방 정보 (기존 또는 새로 생성된)
     * @throws CustomException 자기 자신과 채팅하려고 시도하거나 상대방을 찾을 수 없는 경우
     */
    @Operation(summary = "개인 채팅방 생성/조회", description = "두 사용자 간의 개인 채팅방을 생성하거나 기존 채팅방을 조회합니다.")
    @PostMapping("/rooms/private/{targetMemberId}")
    public CommonResponse<ChatRoomDto> getOrCreatePrivateChatRoom(
            @PathVariable Long targetMemberId,
            @AuthenticationPrincipal Long memberId) {

        ChatRoomDto chatRoom = chatRoomService.getOrCreatePrivateChatRoom(memberId, targetMemberId);
        return CommonResponse.success("개인 채팅방 생성/조회 성공", chatRoom);
    }

    /**
     * 프로젝트 내에서 주제별 채팅방을 생성합니다.
     *
     * <p>특정 주제나 기능에 대해 논의하기 위한 별도의 채팅방을 생성합니다.
     * 예: 'UI/UX 논의', '백엔드 개발', '배포 관련' 등
     * 생성자가 자동으로 첫 번째 멤버가 되며, 요청에 포함된 초기 멤버들도 함께 초대됩니다.</p>
     *
     * @param request 채팅방 생성 요청 (이름, 설명, 프로젝트 ID, 초기 멤버 등)
     * @param memberId 현재 로그인한 사용자 ID (채팅방 생성자가 됨)
     * @return 생성된 주제별 채팅방 정보
     * @throws CustomException 프로젝트 ID가 없거나 프로젝트를 찾을 수 없는 경우
     */
    @Operation(summary = "주제별 채팅방 생성", description = "프로젝트 내에서 주제별 채팅방을 생성합니다.")
    @PostMapping("/rooms/topic")
    public CommonResponse<ChatRoomDto> createTopicChatRoom(
            @Valid @RequestBody ChatRequestDto.CreateChatRoom request,
            @AuthenticationPrincipal Long memberId) {

        ChatRoomDto chatRoom = chatRoomService.createTopicChatRoom(request, memberId);
        return CommonResponse.success("주제별 채팅방 생성 성공", chatRoom);
    }

    /**
     * 기존 채팅방에 새로운 멤버들을 초대합니다.
     *
     * <p>채팅방의 멤버만 다른 사용자를 초대할 수 있으며,
     * 초대된 멤버들에게는 입장 시스템 메시지가 자동으로 전송됩니다.</p>
     *
     * @param chatRoomId 멤버를 초대할 채팅방 ID
     * @param request 초대할 멤버들의 ID 목록
     * @param memberId 현재 로그인한 사용자 ID (초대 권한 확인용)
     * @return 성공 응답
     * @throws CustomException 채팅방을 찾을 수 없거나 초대 권한이 없는 경우
     */
    @Operation(summary = "채팅방에 멤버 초대", description = "기존 채팅방에 새로운 멤버를 초대합니다.")
    @PostMapping("/rooms/{chatRoomId}/invite")
    public CommonResponse<Void> inviteMembers(
            @PathVariable Long chatRoomId,
            @Valid @RequestBody ChatRequestDto.InviteMembers request,
            @AuthenticationPrincipal Long memberId) {

        // 권한 확인 후 멤버 초대
        chatRoomService.getChatRoomDetail(chatRoomId, memberId); // 권한 확인용

        request.getMemberIds().forEach(memberIdToInvite ->
            chatRoomService.addMemberToChatRoom(chatRoomId, memberIdToInvite));

        return CommonResponse.success("멤버 초대 성공");
    }

    /**
     * 특정 채팅방의 메시지 목록을 페이징으로 조회합니다.
     *
     * <p>커서 기반 페이징을 사용하여 효율적인 메시지 로딩을 제공합니다:</p>
     * <ul>
     *   <li>cursor가 null이면 최신 메시지부터 조회</li>
     *   <li>cursor가 있으면 해당 메시지 이전의 메시지들 조회</li>
     *   <li>삭제된 메시지는 제외</li>
     *   <li>최신 메시지가 맨 위에 오도록 정렬</li>
     * </ul>
     *
     * @param chatRoomId 메시지를 조회할 채팅방 ID
     * @param cursor 페이징 커서 (마지막으로 읽은 메시지 ID, null이면 첫 페이지)
     * @param size 한 번에 가져올 메시지 수 (기본값: 20)
     * @param memberId 현재 로그인한 사용자 ID (접근 권한 확인용)
     * @return 메시지 목록
     * @throws CustomException 채팅방 접근 권한이 없는 경우
     */
    @Operation(summary = "채팅 메시지 조회", description = "특정 채팅방의 메시지 목록을 페이징으로 조회합니다.")
    @GetMapping("/rooms/{chatRoomId}/messages")
    public CommonResponse<List<ChatMessageDto>> getChatMessages(
            @PathVariable Long chatRoomId,
            @Parameter(description = "페이징 커서 (마지막 메시지 ID)") @RequestParam(required = false) Long cursor,
            @Parameter(description = "한 번에 가져올 메시지 수 (기본 20)") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Long memberId) {

        List<ChatMessageDto> messages = chatMessageService.getChatMessages(chatRoomId, cursor, size, memberId);
        return CommonResponse.success("채팅 메시지 조회 성공", messages);
    }

    /**
     * 특정 채팅방의 읽음 상태를 업데이트합니다.
     *
     * <p>사용자가 채팅방에 입장하거나 메시지를 확인했을 때 호출되어:
     * <ul>
     *   <li>마지막 읽은 시간을 현재 시간으로 업데이트</li>
     *   <li>읽지 않은 메시지 수를 0으로 초기화</li>
     * </ul>
     * </p>
     *
     * @param chatRoomId 읽음 상태를 업데이트할 채팅방 ID
     * @param memberId 현재 로그인한 사용자 ID
     * @return 성공 응답
     * @throws CustomException 채팅방 접근 권한이 없는 경우
     */
    @Operation(summary = "읽음 상태 업데이트", description = "특정 채팅방의 읽음 상태를 업데이트합니다.")
    @PostMapping("/rooms/{chatRoomId}/read")
    public CommonResponse<Void> updateReadStatus(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal Long memberId) {

        ChatRequestDto.UpdateReadStatus request = ChatRequestDto.UpdateReadStatus.builder()
                .chatRoomId(chatRoomId)
                .build();

        chatMessageService.updateReadStatus(chatRoomId, memberId);
        return CommonResponse.success("읽음 상태 업데이트 성공");
    }

    /**
     * 자신이 작성한 메시지를 수정합니다.
     *
     * <p>메시지 수정 조건:</p>
     * <ul>
     *   <li>작성자 본인만 수정 가능</li>
     *   <li>삭제되지 않은 메시지만 수정 가능</li>
     *   <li>수정 시간과 수정 플래그 자동 업데이트</li>
     *   <li>수정된 메시지는 WebSocket으로 실시간 알림</li>
     * </ul>
     *
     * @param messageId 수정할 메시지 ID
     * @param request 메시지 수정 요청 (새로운 내용)
     * @param memberId 현재 로그인한 사용자 ID (작성자 권한 확인용)
     * @return 수정된 메시지 정보
     * @throws CustomException 메시지를 찾을 수 없거나 수정 권한이 없는 경우, 이미 삭제된 메시지인 경우
     */
    @Operation(summary = "메시지 수정", description = "자신이 작성한 메시지를 수정합니다.")
    @PutMapping("/messages/{messageId}")
    public CommonResponse<ChatMessageDto> editMessage(
            @PathVariable Long messageId,
            @Valid @RequestBody ChatRequestDto.EditMessage request,
            @AuthenticationPrincipal Long memberId) {

        request.setMessageId(messageId);
        ChatMessageDto editedMessage = chatMessageService.editMessage(request, memberId);
        return CommonResponse.success("메시지 수정 성공", editedMessage);
    }

    /**
     * 자신이 작성한 메시지를 삭제합니다.
     *
     * <p>메시지 삭제는 소프트 삭제 방식으로 처리됩니다:</p>
     * <ul>
     *   <li>실제 데이터는 보존</li>
     *   <li>삭제 플래그만 설정</li>
     *   <li>작성자 본인만 삭제 가능</li>
     *   <li>삭제된 메시지는 목록에서 제외</li>
     *   <li>삭제 알림이 WebSocket으로 실시간 전송</li>
     * </ul>
     *
     * @param messageId 삭제할 메시지 ID
     * @param memberId 현재 로그인한 사용자 ID (작성자 권한 확인용)
     * @return 성공 응답
     * @throws CustomException 메시지를 찾을 수 없거나 삭제 권한이 없는 경우
     */
    @Operation(summary = "메시지 삭제", description = "자신이 작성한 메시지를 삭제합니다.")
    @DeleteMapping("/messages/{messageId}")
    public CommonResponse<Void> deleteMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal Long memberId) {

        chatMessageService.deleteMessage(messageId, memberId);
        return CommonResponse.success("메시지 삭제 성공");
    }
}
