package com.wooyeon.table.domain.chat.application.service;

import com.wooyeon.table.domain.chat.domain.model.ChatRoom;
import com.wooyeon.table.domain.chat.domain.repository.ChatRoomRepository;
import com.wooyeon.table.domain.chat.domain.service.MemberIntro;
import com.wooyeon.table.domain.event.application.dto.EventSlotInfo;
import com.wooyeon.table.domain.event.application.service.EventSlotQueryService;
import com.wooyeon.table.domain.matching.application.dto.ConfirmedTable;
import com.wooyeon.table.domain.matching.application.service.MatchingQueryService;
import com.wooyeon.table.domain.profile.application.dto.MatchingProfile;
import com.wooyeon.table.domain.profile.application.service.ProfileQueryService;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 테이블 확정과 동시에 앱 내 단체방을 만들고 참가자 전원을 초대한다 (INF-04).
 * 이미 단체방이 있는 테이블은 건너뛴다 (중복 단체방 금지, PRD 16절)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomService {

    private static final DateTimeFormatter TITLE_DATE = DateTimeFormatter.ofPattern("M/d");

    private final ChatRoomRepository chatRoomRepository;
    private final MatchingQueryService matchingQueryService;
    private final EventSlotQueryService eventSlotQueryService;
    private final ProfileQueryService profileQueryService;

    /** 새로 만든 단체방만 돌려준다 */
    public List<ChatRoom> openForSlot(Long slotId) {
        EventSlotInfo slot = eventSlotQueryService.getSlot(slotId);
        List<ChatRoom> opened = new ArrayList<>();
        for (ConfirmedTable table : matchingQueryService.findConfirmedTables(slotId)) {
            if (chatRoomRepository.findByTableId(table.tableId()).isEmpty()) {
                opened.add(open(table, slot));
            }
        }
        log.info("회차 {} 단체방 {}개 생성", slot.eventDate(), opened.size());
        return opened;
    }

    private ChatRoom open(ConfirmedTable table, EventSlotInfo slot) {
        String title = slot.eventDate().format(TITLE_DATE) + " " + table.region().getLabel() + " "
                + table.tableNo() + "번 테이블";
        ChatRoom room = ChatRoom.open(table.tableId(), table.slotId(), slot.eventDate(), title);
        Map<Long, MatchingProfile> profiles = profileQueryService.findMatchingProfiles(table.userIds());
        for (int i = 0; i < table.userIds().size(); i++) {
            Long userId = table.userIds().get(i);
            MatchingProfile profile = profiles.get(userId);
            String intro = profile == null ? "" : MemberIntro.of(profile.ageIn(slot.eventDate().getYear()),
                    profile.mbti(), profile.interests(), profile.traits());
            room.invite(userId, table.applicationIds().get(i), MemberIntro.alias(i + 1), intro);
        }
        return chatRoomRepository.save(room);
    }
}
