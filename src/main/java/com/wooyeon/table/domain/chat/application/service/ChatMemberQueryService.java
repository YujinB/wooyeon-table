package com.wooyeon.table.domain.chat.application.service;

import com.wooyeon.table.domain.chat.application.dto.TableMember;
import com.wooyeon.table.domain.chat.domain.repository.ChatRoomRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 다른 도메인(후기·신고)이 단체방 별칭으로 같은 테이블 사람을 고를 때 쓴다 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMemberQueryService {

    private final ChatRoomRepository chatRoomRepository;

    /** 테이블 단체방 멤버 (좌석 순). 단체방이 없으면 빈 목록 */
    public List<TableMember> membersOfTable(Long tableId) {
        return chatRoomRepository.findByTableId(tableId)
                .map(room -> room.getMembers().stream()
                        .map(member -> new TableMember(member.getSeatNo(), member.getAlias(), member.getIntro(),
                                member.getUserId(), member.getApplicationId()))
                        .toList())
                .orElse(List.of());
    }
}
