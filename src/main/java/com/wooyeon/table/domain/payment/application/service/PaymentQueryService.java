package com.wooyeon.table.domain.payment.application.service;

import com.wooyeon.table.domain.payment.application.dto.PaymentRecordView;
import com.wooyeon.table.domain.payment.domain.repository.PaymentRecordRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 다른 도메인(운영 조회)이 결제·환불 내역을 볼 때 쓴다 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentQueryService {

    private final PaymentRecordRepository paymentRecordRepository;

    /** 최근 결제·환불 500건 (최신부터) */
    public List<PaymentRecordView> findRecent() {
        return paymentRecordRepository.findRecent().stream().map(PaymentRecordView::from).toList();
    }
}
