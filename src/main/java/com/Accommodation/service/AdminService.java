package com.Accommodation.service;

import com.Accommodation.entity.Member;
import com.Accommodation.entity.Order;
import com.Accommodation.repository.MemberRepository;
import com.Accommodation.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminService {

    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;

    public Page<Member> getMemberPage(Pageable pageable) {
        return memberRepository.findAll(pageable);
    }

    public Page<Order> getOrderPage(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }
}
