package com.zmj.gbs_commerce_system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmj.gbs_commerce_system.entity.Member;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface MemberService {

    List<Member> findAll();

    IPage<Member> findMembersWithPagination(Page<Member> page, Map<String, Object> queryParams);

    Member findById(Long id);

    Member findByMemberId(String memberId);

    boolean createMember(Member member);

    boolean updateMember(Member member);

    boolean deleteMember(Long id);

    boolean deductBalance(String memberId, BigDecimal amount);

    boolean addBalance(String memberId, BigDecimal amount);
}
