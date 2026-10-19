package com.wooyeon.table.domain.payment.presentation.response;

import java.util.List;

public record RefundPolicyResponse(String summary, List<String> rules) {
}
