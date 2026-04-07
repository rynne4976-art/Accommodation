package com.Accommodation.service;

import com.Accommodation.constant.Role;
import com.Accommodation.dto.MemberSearchDto;
import com.Accommodation.dto.OrderSearchDto;
import com.Accommodation.entity.Accom;
import com.Accommodation.entity.Member;
import com.Accommodation.entity.Order;
import com.Accommodation.exception.AdminException;
import com.Accommodation.exception.ErrorCode;
import com.Accommodation.repository.AccomRepository;
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
    private final AccomRepository accomRepository;
    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;

    public Page<Member> getMemberPage(MemberSearchDto memberSearchDto, String currentAdminEmail, Pageable pageable) {
        String searchBy = memberSearchDto.getSearchBy();
        if (searchBy == null || searchBy.isBlank()) {
            searchBy = "all";
        }

        String searchQuery = memberSearchDto.getSearchQuery();
        if (searchQuery != null) {
            searchQuery = searchQuery.trim();
        }

        return memberRepository.searchMembers(
                searchBy,
                searchQuery,
                memberSearchDto.getRole(),
                currentAdminEmail,
                pageable
        );
    }

    public Page<Order> getOrderPage(OrderSearchDto orderSearchDto, Pageable pageable) {
        return orderRepository.searchOrders(orderSearchDto, pageable);
    }

    public Member getMemberDetail(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new AdminException(ErrorCode.ADMIN_MEMBER_NOT_FOUND));
    }

    public long getMemberCount() {
        return memberRepository.count();
    }

    public long getAdminCount() {
        return memberRepository.countByRole(Role.ADMIN);
    }

    public long getAccomCount() {
        return accomRepository.count();
    }

    public long getOrderCount() {
        return orderRepository.count();
    }

    @Transactional
    public void updateMemberRole(Long memberId, Role role, String currentAdminEmail) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new AdminException(ErrorCode.ADMIN_MEMBER_NOT_FOUND));

        if (member.getEmail().equals(currentAdminEmail) && role != Role.ADMIN) {
            throw new AdminException(ErrorCode.ADMIN_ROLE_DOWNGRADE_FORBIDDEN);
        }

        member.setRole(role);
    }
}
