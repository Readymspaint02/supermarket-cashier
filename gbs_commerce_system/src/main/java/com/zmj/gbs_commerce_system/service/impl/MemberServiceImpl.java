package com.zmj.gbs_commerce_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmj.gbs_commerce_system.entity.Member;
import com.zmj.gbs_commerce_system.mapper.MemberMapper;
import com.zmj.gbs_commerce_system.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class MemberServiceImpl implements MemberService {

    @Autowired
    private MemberMapper memberMapper;

    @Override
    public List<Member> findAll() {
        return memberMapper.selectList(new QueryWrapper<Member>().orderByDesc("create_time"));
    }

    @Override
    public IPage<Member> findMembersWithPagination(Page<Member> page, Map<String, Object> queryParams) {
        QueryWrapper<Member> wrapper = new QueryWrapper<>();
        if (queryParams != null) {
            Object memberId = queryParams.get("memberId");
            if (memberId != null && !"".equals(memberId)) {
                wrapper.like("member_id", memberId);
            }
            Object name = queryParams.get("name");
            if (name != null && !"".equals(name)) {
                wrapper.like("name", name);
            }
            Object phone = queryParams.get("phone");
            if (phone != null && !"".equals(phone)) {
                wrapper.like("phone", phone);
            }
            Object level = queryParams.get("level");
            if (level != null && !"".equals(level)) {
                wrapper.eq("level", level);
            }
            Object status = queryParams.get("status");
            if (status != null && !"".equals(status)) {
                wrapper.eq("status", status);
            }
        }
        wrapper.orderByDesc("create_time");
        return memberMapper.selectPage(page, wrapper);
    }

    @Override
    public Member findById(Long id) {
        return memberMapper.selectById(id);
    }

    @Override
    public Member findByMemberId(String memberId) {
        if (memberId == null || memberId.isEmpty()) {
            return null;
        }
        return memberMapper.selectByMemberId(memberId);
    }

    @Override
    @Transactional
    public boolean createMember(Member member) {
        return memberMapper.insert(member) > 0;
    }

    @Override
    @Transactional
    public boolean updateMember(Member member) {
        return memberMapper.updateById(member) > 0;
    }

    @Override
    @Transactional
    public boolean deleteMember(Long id) {
        return memberMapper.deleteById(id) > 0;
    }
}
