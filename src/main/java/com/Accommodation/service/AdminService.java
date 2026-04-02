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

    // 관리자 화면은 조회 중심이라 별도 서비스로 묶어 책임을 분리했습니다.
    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;

    public Page<Member> getMemberPage(Pageable pageable) {
        return memberRepository.findAll(pageable);
    }

    public Page<Order> getOrderPage(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }
}
