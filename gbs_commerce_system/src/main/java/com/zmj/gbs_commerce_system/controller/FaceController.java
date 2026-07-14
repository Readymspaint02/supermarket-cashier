package com.zmj.gbs_commerce_system.controller;

import com.zmj.gbs_commerce_system.annotation.RateLimit;
import com.zmj.gbs_commerce_system.config.BaiduAiProperties;
import com.zmj.gbs_commerce_system.dto.FaceRegisterRequest;
import com.zmj.gbs_commerce_system.entity.Member;
import com.zmj.gbs_commerce_system.service.FaceAuthService;
import com.zmj.gbs_commerce_system.service.FaceAuthService.RegisterResult;
import com.zmj.gbs_commerce_system.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "人脸识别接口")
@RestController
@RequestMapping("/face")
public class FaceController {

    @Autowired
    private FaceAuthService faceAuthService;
    
    @Autowired
    private BaiduAiProperties baiduAiProperties;
    
    @Autowired
    private MemberService memberService;

    @GetMapping("/config")
    @Operation(summary = "检查百度AI配置")
    public Map<String, Object> checkConfig() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", Map.of(
            "apiKey", baiduAiProperties.getApiKey() != null && !baiduAiProperties.getApiKey().isEmpty() ? "已配置" : "未配置",
            "secretKey", baiduAiProperties.getSecretKey() != null && !baiduAiProperties.getSecretKey().isEmpty() ? "已配置" : "未配置",
            "groupId", baiduAiProperties.getGroupId() != null ? baiduAiProperties.getGroupId() : "未配置"
        ));
        return result;
    }

    @GetMapping("/check")
    @Operation(summary = "检查用户人脸注册状态")
    public Map<String, Object> checkFaceRegistered(@RequestParam String userId) {
        Map<String, Object> result = new HashMap<>();
        
        if (userId == null || userId.isEmpty()) {
            result.put("code", 400);
            result.put("msg", "用户ID不能为空");
            return result;
        }
        
        boolean registered = faceAuthService.checkFaceRegistered(userId);
        result.put("code", 200);
        result.put("data", Map.of("registered", registered));
        return result;
    }

    @PostMapping("/register")
    @Operation(summary = "人脸注册")
    @RateLimit(value = 5, windowSeconds = 60, limitType = RateLimit.LimitType.IP, message = "人脸注册请求过于频繁，请稍后再试")
    public Map<String, Object> registerFace(@Validated @RequestBody FaceRegisterRequest request) {
        Map<String, Object> result = new HashMap<>();
        
        RegisterResult registerResult = faceAuthService.registerFace(
                request.getUserId(),
                request.getImage(),
                request.getUserInfo()
        );
        
        if (registerResult.isSuccess()) {
            Member member = memberService.findByMemberId(request.getUserId());
            if (member != null) {
                member.setFaceRegistered(1);
                memberService.updateMember(member);
            }
            result.put("code", 200);
            result.put("msg", "人脸注册成功");
        } else {
            result.put("code", 500);
            result.put("msg", registerResult.getMessage());
        }
        return result;
    }

    @PostMapping("/verify")
    @Operation(summary = "人脸验证")
    @RateLimit(value = 10, windowSeconds = 60, limitType = RateLimit.LimitType.IP, message = "人脸验证请求过于频繁，请稍后再试")
    public Map<String, Object> verifyFace(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        
        String userId = body.get("userId");
        String image = body.get("image");
        
        if (userId == null || userId.isEmpty()) {
            result.put("code", 400);
            result.put("msg", "用户ID不能为空");
            return result;
        }
        
        if (image == null || image.isEmpty()) {
            result.put("code", 400);
            result.put("msg", "人脸图像不能为空");
            return result;
        }
        
        boolean verified = faceAuthService.verifyFaceForMember(image, userId);
        
        if (verified) {
            result.put("code", 200);
            result.put("msg", "人脸验证成功");
        } else {
            result.put("code", 500);
            result.put("msg", "人脸验证失败，请确保是本人操作");
        }
        return result;
    }
}