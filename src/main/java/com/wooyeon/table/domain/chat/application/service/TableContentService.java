package com.wooyeon.table.domain.chat.application.service;

import com.wooyeon.table.domain.chat.domain.model.ChatRoom;
import com.wooyeon.table.domain.chat.domain.repository.ChatRoomRepository;
import com.wooyeon.table.domain.chat.domain.service.TableContentGenerator;
import com.wooyeon.table.domain.chat.domain.service.TableContentGenerator.MemberSignal;
import com.wooyeon.table.domain.chat.domain.service.TableContentGenerator.TableContent;
import com.wooyeon.table.domain.matching.application.dto.ConfirmedTable;
import com.wooyeon.table.domain.matching.application.service.MatchingQueryService;
import com.wooyeon.table.domain.profile.application.dto.MatchingProfile;
import com.wooyeon.table.domain.profile.application.service.ProfileQueryService;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 테이블 대화 주제·아이스브레이킹 (CON-01·02). 멤버 프로필에서 그때그때 만든다 (같은 멤버면 같은 결과) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TableContentService {

    private final MatchingQueryService matchingQueryService;
    private final ProfileQueryService profileQueryService;
    private final ChatRoomRepository chatRoomRepository;

    public TableContent forTable(Long tableId) {
        ConfirmedTable table = matchingQueryService.getConfirmedTable(tableId);
        List<MemberSignal> members = profileQueryService.findMatchingProfiles(table.userIds()).values().stream()
                .map(TableContentService::signal)
                .toList();
        return TableContentGenerator.generate(members);
    }

    /** 단체방 멤버만 */
    public TableContent forRoom(Long userId, Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .filter(found -> found.hasMember(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND, String.valueOf(roomId)));
        return forTable(room.getTableId());
    }

    private static MemberSignal signal(MatchingProfile profile) {
        return new MemberSignal(profile.interests(), profile.traits(), profile.mbti());
    }
}
