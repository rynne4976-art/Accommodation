package com.Accommodation.repository;

import com.Accommodation.constant.OrderStatus;
import com.Accommodation.dto.OrderSearchDto;
import com.Accommodation.entity.Order;
import com.Accommodation.entity.QMember;
import com.Accommodation.entity.QOrder;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class OrderRepositoryCustomImpl implements OrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public OrderRepositoryCustomImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Page<Order> searchOrders(OrderSearchDto orderSearchDto, Pageable pageable) {
        QOrder order = QOrder.order;
        QMember member = QMember.member;

        List<Order> content = queryFactory
                .selectFrom(order)
                .leftJoin(order.member, member).fetchJoin()
                .where(
                        searchCondition(orderSearchDto),
                        orderStatusEq(orderSearchDto.getOrderStatus())
                )
                .orderBy(order.orderDate.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(order.count())
                .from(order)
                .leftJoin(order.member, member)
                .where(
                        searchCondition(orderSearchDto),
                        orderStatusEq(orderSearchDto.getOrderStatus())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    private BooleanExpression orderStatusEq(OrderStatus orderStatus) {
        return orderStatus == null ? null : QOrder.order.orderStatus.eq(orderStatus);
    }

    private BooleanBuilder searchCondition(OrderSearchDto orderSearchDto) {
        BooleanBuilder builder = new BooleanBuilder();
        String searchBy = normalizeSearchBy(orderSearchDto.getSearchBy());
        String searchQuery = normalizeSearchQuery(orderSearchDto.getSearchQuery());

        if (searchQuery == null) {
            return builder;
        }

        if ("orderId".equals(searchBy)) {
            Long orderId = parseOrderId(searchQuery);
            return orderId == null ? builder.and(QOrder.order.id.eq(-1L)) : builder.and(QOrder.order.id.eq(orderId));
        }

        if ("email".equals(searchBy)) {
            return builder.and(QOrder.order.member.email.containsIgnoreCase(searchQuery));
        }

        if ("memberName".equals(searchBy)) {
            return builder.and(QOrder.order.member.name.containsIgnoreCase(searchQuery));
        }

        Long orderId = parseOrderId(searchQuery);
        BooleanBuilder allBuilder = new BooleanBuilder();
        allBuilder.or(QOrder.order.member.email.containsIgnoreCase(searchQuery));
        allBuilder.or(QOrder.order.member.name.containsIgnoreCase(searchQuery));
        if (orderId != null) {
            allBuilder.or(QOrder.order.id.eq(orderId));
        }

        return builder.and(allBuilder);
    }

    private String normalizeSearchBy(String searchBy) {
        if (searchBy == null || searchBy.isBlank()) {
            return "all";
        }
        return searchBy;
    }

    private String normalizeSearchQuery(String searchQuery) {
        if (searchQuery == null) {
            return null;
        }

        String trimmed = searchQuery.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Long parseOrderId(String searchQuery) {
        try {
            return Long.parseLong(searchQuery);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
