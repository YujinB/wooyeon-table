package com.wooyeon.table.domain.chat.application.dto;

import java.util.List;
import java.util.Optional;

public record RoomMembers(Long roomId, Long tableId, Long slotId, List<TableMember> members) {

    public Optional<TableMember> seat(int seatNo) {
        return members.stream().filter(member -> member.seatNo() == seatNo).findFirst();
    }
}
