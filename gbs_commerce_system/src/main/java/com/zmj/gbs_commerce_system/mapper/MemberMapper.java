package com.zmj.gbs_commerce_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmj.gbs_commerce_system.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MemberMapper extends BaseMapper<Member> {

    @Select("SELECT * FROM members WHERE member_id = #{memberId} LIMIT 1")
    Member selectByMemberId(String memberId);
}
