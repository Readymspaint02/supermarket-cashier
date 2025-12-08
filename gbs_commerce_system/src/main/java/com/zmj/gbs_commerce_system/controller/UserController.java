package com.zmj.gbs_commerce_system.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmj.gbs_commerce_system.entity.User;
import com.zmj.gbs_commerce_system.service.FileUploadService;
import com.zmj.gbs_commerce_system.service.UserService;
import com.zmj.gbs_commerce_system.utils.PageParams;
import com.zmj.gbs_commerce_system.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/user")
@Tag(name = "用户管理接口")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/add")
    @Operation(summary = "新增用户")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "新增成功"),
            @ApiResponse(responseCode = "500", description = "新增失败")
    })
    public Map<String, Object> addUser(
            @Parameter(description = "用户信息") @Valid @RequestBody User user) {
        // 检查用户名是否已存在
        if (userService.findByUsername(user.getUsername()) != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("msg", "用户名已存在");
            return result;
        }

        boolean success = userService.saveUser(user);
        Map<String, Object> result = new HashMap<>();
        if (success) {
            result.put("code", 200);
            result.put("msg", "新增成功");
        } else {
            result.put("code", 500);
            result.put("msg", "新增失败");
        }
        return result;
    }
    /**
     * 修改用户密码
     *
     * @param id 用户ID
     * @param requestBody 请求体，包含旧密码和新密码
     * @return 修改结果
     */
    @PostMapping("/{id}/password")
    public Map<String, Object> updatePassword(@PathVariable Long id, @RequestBody Map<String, String> requestBody) {
        String oldPassword = requestBody.get("oldPassword");
        String newPassword = requestBody.get("newPassword");

        if (oldPassword == null || newPassword == null || oldPassword.isEmpty() || newPassword.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("msg", "旧密码和新密码不能为空");
            return result;
        }

        boolean updated = userService.updateUserPassword(id, oldPassword, newPassword);

        Map<String, Object> result = new HashMap<>();
        if (updated) {
            result.put("code", 200);
            result.put("msg", "密码修改成功");
        } else {
            result.put("code", 500);
            result.put("msg", "密码修改失败，旧密码错误或用户不存在");
        }

        return result;
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前登录用户信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "500", description = "用户未登录")
    })
    public Map<String, Object> getCurrentUserInfo() {
        User currentUser = SecurityUtils.getCurrentUser();
        Map<String, Object> result = new HashMap<>();
        if (currentUser != null) {
            // 重新从数据库获取用户信息，确保信息是最新的
            User user = userService.findUserById(currentUser.getId());
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("data", user);
        } else {
            result.put("code", 500);
            result.put("msg", "用户未登录");
        }
        return result;
    }

    @GetMapping("/list")
    @Operation(summary = "获取所有用户列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public Map<String, Object> getAllUsers() {
        List<User> users = userService.findAllUsers();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", users);
        return result;
    }
    
    @PostMapping("/page")
    @Operation(summary = "分页获取用户列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public Map<String, Object> getUsersWithPagination(
            @RequestBody PageParams pageParams) {
        Integer pageNum = pageParams.getPageNum()!=null?pageParams.getPageNum():1;
        Integer pageSize = pageParams.getPageSize()!=null?pageParams.getPageSize():10;
        Page<User> page = new Page<>(pageNum, pageSize);
        IPage<User> userPage = userService.findUsersWithPagination(page,pageParams.getQueryParams());
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", userPage);
        return result;
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取用户信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "500", description = "用户不存在")
    })
    public Map<String, Object> getUserById(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        User user = userService.findUserById(id);
        Map<String, Object> result = new HashMap<>();
        if (user != null) {
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("data", user);
        } else {
            result.put("code", 500);
            result.put("msg", "用户不存在");
        }
        return result;
    }

    @PutMapping("/update/{id}")
    @Operation(summary = "更新用户信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "500", description = "更新失败")
    })
    public Map<String, Object> updateUser(@PathVariable Long id,
            @Parameter(description = "用户信息") @Valid @RequestBody User user) {
        user.setId(id);
        boolean success = userService.updateUser(user);
        Map<String, Object> result = new HashMap<>();
        if (success) {
            result.put("code", 200);
            result.put("msg", "更新成功");
        } else {
            result.put("code", 500);
            result.put("msg", "更新失败");
        }
        return result;
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除用户")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "500", description = "删除失败")
    })
    public Map<String, Object> deleteUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        boolean success = userService.deleteUserById(id);
        Map<String, Object> result = new HashMap<>();
        if (success) {
            result.put("code", 200);
            result.put("msg", "删除成功");
        } else {
            result.put("code", 500);
            result.put("msg", "删除失败");
        }
        return result;
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "修改用户密码")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "密码修改成功"),
            @ApiResponse(responseCode = "500", description = "密码修改失败")
    })
    public Map<String, Object> changePassword(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "新密码") @RequestBody Map<String, String> payload) {
        String newPassword = payload.get("newPassword");
        if (newPassword == null || newPassword.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("msg", "新密码不能为空");
            return result;
        }

        boolean success = userService.changePassword(id, newPassword);
        Map<String, Object> result = new HashMap<>();
        if (success) {
            result.put("code", 200);
            result.put("msg", "密码修改成功");
        } else {
            result.put("code", 500);
            result.put("msg", "密码修改失败");
        }
        return result;
    }
    @Autowired
    private FileUploadService fileUploadService;
    @PostMapping("/updateAvatar")
    public Map<String, Object> updateAvatar(@RequestParam("file")MultipartFile multipartFile){
        String updateFileName = multipartFile.getOriginalFilename();
        Map<String, Object> result = new HashMap<>();
        if (StrUtil.isBlank(updateFileName)) {
            result.put("code", 500);
            result.put("msg", "上传文件不能为空");
            return result;
        }
        try {
            String uploadFileName = fileUploadService.uploadFile(multipartFile);
            if (uploadFileName != null){
                User currentUser = SecurityUtils.getCurrentUser();
                if (currentUser!=null) {
                    User user = userService.findUserById(currentUser.getId());
                    user.setAvatar("/uploads/"+uploadFileName);
                    userService.updateUserById(user);
                    result.put("code", 200);
                    result.put("msg", "上传成功");
                    result.put("data", "/uploads/"+uploadFileName);
                }else{
                    result.put("code", 500);
                    result.put("msg", "当前用户没有登录");
                }
            }
        } catch (IOException e) {
            result.put("code", 500);
            result.put("msg", "上传失败");
        }
        return result;
    }
    @GetMapping("/perms")
    public Map<String, Object> getCurrentUserPerms() {
        User currentUser = SecurityUtils.getCurrentUser();
        Map<String, Object> result = new HashMap<>();
        if (currentUser != null) {
            List<String> perms = userService.getPermsByUserId(currentUser.getId());
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("data", perms);
        } else {
            result.put("code", 500);
            result.put("msg", "当前用户没有登录");
        }
        return result;
    }
}