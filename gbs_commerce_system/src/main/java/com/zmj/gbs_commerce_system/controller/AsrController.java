package com.zmj.gbs_commerce_system.controller;

import com.zmj.gbs_commerce_system.annotation.RateLimit;
import com.zmj.gbs_commerce_system.dto.AsrRequest;
import com.zmj.gbs_commerce_system.metrics.BusinessMetrics;
import com.zmj.gbs_commerce_system.service.AsrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/asr")
@RequiredArgsConstructor
@Tag(name = "语音识别接口")
public class AsrController {

    private final AsrService asrService;
    private final BusinessMetrics businessMetrics;

    @PostMapping("/recognize")
    @Operation(summary = "一句话语音识别")
    @RateLimit(value = 20, windowSeconds = 60, limitType = RateLimit.LimitType.IP_AND_USER, message = "语音识别请求过于频繁，请稍后再试")
    public Map<String, Object> recognize(@RequestBody AsrRequest request) {
        businessMetrics.incrementAsrCall();
        
        try {
            String text = asrService.recognize(
                    request.getAudioBase64(),
                    request.getAudioFormat(),
                    request.getProperty()
            );
            businessMetrics.incrementAsrSuccess();
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("msg", "识别成功");
            result.put("data", text);
            return result;
        } catch (Exception e) {
            businessMetrics.incrementAsrFail();
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("msg", "识别失败：" + e.getMessage());
            return result;
        }
    }
}