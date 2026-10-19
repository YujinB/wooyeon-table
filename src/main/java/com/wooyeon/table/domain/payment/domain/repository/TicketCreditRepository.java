package com.wooyeon.table.domain.payment.domain.repository;

import com.wooyeon.table.domain.payment.domain.model.TicketCredit;
import java.util.Optional;

public interface TicketCreditRepository {

    TicketCredit save(TicketCredit credit);

    /** 가장 먼저 보관한, 아직 쓰지 않은 보관 이용권 */
    Optional<TicketCredit> findFirstUnused(Long userId);
}
