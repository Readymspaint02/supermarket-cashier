package com.zmj.gbs_commerce_system.controller;

import com.zmj.gbs_commerce_system.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/report")
@Tag(name = "报表统计接口")
public class ReportController {

    @Autowired
    private ReportService reportService;

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", data);
        return result;
    }

    @RequiresPermissions("report:sales:view")
    @GetMapping("/sales/overview")
    @Operation(summary = "销售概览")
    public Map<String, Object> salesOverview() {
        return success(reportService.getSalesOverview());
    }

    @RequiresPermissions("report:sales:view")
    @GetMapping("/sales/trend")
    @Operation(summary = "销售趋势（最近7天）")
    public Map<String, Object> salesTrend(
            @RequestParam(value = "days", required = false, defaultValue = "7") Integer days) {
        List<Map<String, Object>> data = reportService.getSalesTrend(days == null ? 7 : days);
        return success(data);
    }

    @RequiresPermissions("report:inventory:alert")
    @GetMapping("/inventory/warning")
    @Operation(summary = "库存预警列表")
    public Map<String, Object> inventoryWarning() {
        return success(reportService.getInventoryWarnings());
    }
}
