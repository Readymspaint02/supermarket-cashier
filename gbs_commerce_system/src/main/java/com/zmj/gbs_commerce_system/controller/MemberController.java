package com.zmj.gbs_commerce_system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmj.gbs_commerce_system.entity.Member;
import com.zmj.gbs_commerce_system.service.MemberService;
import com.zmj.gbs_commerce_system.utils.PageParams;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/member")
@Tag(name = "会员管理接口")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @GetMapping("/list")
    @Operation(summary = "获取全部会员列表")
    public Map<String, Object> listAll() {
        List<Member> members = memberService.findAll();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", members);
        return result;
    }

    @PostMapping("/page")
    @Operation(summary = "分页查询会员")
    public Map<String, Object> page(@RequestBody PageParams pageParams) {
        Integer pageNum = pageParams.getPageNum() != null ? pageParams.getPageNum() : 1;
        Integer pageSize = pageParams.getPageSize() != null ? pageParams.getPageSize() : 10;
        Page<Member> page = new Page<>(pageNum, pageSize);
        IPage<Member> memberPage = memberService.findMembersWithPagination(page, pageParams.getQueryParams());
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", memberPage);
        return result;
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询会员")
    public Map<String, Object> detail(@PathVariable Long id) {
        Member member = memberService.findById(id);
        Map<String, Object> result = new HashMap<>();
        if (member == null) {
            result.put("code", 500);
            result.put("msg", "会员不存在");
        } else {
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("data", member);
        }
        return result;
    }

    @GetMapping("/byMemberId/{memberId}")
    @Operation(summary = "根据会员编号查询会员")
    public Map<String, Object> getByMemberId(@PathVariable String memberId) {
        Map<String, Object> result = new HashMap<>();
        if (memberId == null || memberId.isEmpty()) {
            result.put("code", 500);
            result.put("msg", "会员编号不能为空");
            return result;
        }
        Member member = memberService.findByMemberId(memberId);
        if (member == null) {
            result.put("code", 500);
            result.put("msg", "会员不存在");
        } else {
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("data", member);
        }
        return result;
    }

    @RequiresPermissions("system:member:add")
    @PostMapping("/add")
    @Operation(summary = "新增会员")
    public Map<String, Object> add(@RequestBody Member member) {
        Map<String, Object> result = new HashMap<>();
        if (memberService.findByMemberId(member.getMemberId()) != null) {
            result.put("code", 500);
            result.put("msg", "会员编号已存在");
            return result;
        }
        boolean success = memberService.createMember(member);
        result.put("code", success ? 200 : 500);
        result.put("msg", success ? "新增成功" : "新增失败");
        return result;
    }

    @RequiresPermissions("system:member:edit")
    @PutMapping("/update/{id}")
    @Operation(summary = "编辑会员")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Member member) {
        member.setId(id);
        Map<String, Object> result = new HashMap<>();
        boolean success = memberService.updateMember(member);
        result.put("code", success ? 200 : 500);
        result.put("msg", success ? "更新成功" : "更新失败");
        return result;
    }

    @RequiresPermissions("system:member:delete")
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除会员")
    public Map<String, Object> delete(@Parameter(description = "会员ID") @PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        boolean success = memberService.deleteMember(id);
        result.put("code", success ? 200 : 500);
        result.put("msg", success ? "删除成功" : "删除失败");
        return result;
    }
}
