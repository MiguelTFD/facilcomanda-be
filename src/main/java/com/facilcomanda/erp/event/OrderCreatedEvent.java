package com.facilcomanda.erp.event;

import com.facilcomanda.erp.model.Order;

public record OrderCreatedEvent(Order order) {}