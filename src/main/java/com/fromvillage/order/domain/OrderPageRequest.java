package com.fromvillage.order.domain;

public record OrderPageRequest(
        int page,
        int size,
        OrderQuerySort sort
) {
}
