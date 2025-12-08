package com.zmj.gbs_commerce_system.service.impl;

import com.zmj.gbs_commerce_system.mapper.ReportMapper;
import com.zmj.gbs_commerce_system.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportMapper reportMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public Map<String, Object> getSalesOverview() {
        Map<String, Object> overview = new HashMap<>(reportMapper.getSalesOverview());
        overview.put("topProducts", reportMapper.getTopProducts());
        Integer memberCount = reportMapper.getMemberCount();
        overview.put("totalMembers", memberCount == null ? 0 : memberCount);
        return overview;
    }

    @Override
    public List<Map<String, Object>> getSalesTrend(int days) {
        int range = Math.max(1, Math.min(days, 30));
        List<Map<String, Object>> raw = reportMapper.getSalesTrendRaw(range - 1);
        Map<String, Map<String, Object>> rawMap = new HashMap<>();
        if (raw != null) {
            for (Map<String, Object> entry : raw) {
                rawMap.put(String.valueOf(entry.get("day")), entry);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = range - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            String key = day.format(DATE_FORMATTER);
            Map<String, Object> data = rawMap.getOrDefault(key, null);
            Map<String, Object> row = new HashMap<>();
            row.put("day", key);
            row.put("totalSales", data == null ? 0 : data.get("totalSales"));
            row.put("orderCount", data == null ? 0 : data.get("orderCount"));
            result.add(row);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getInventoryWarnings() {
        return reportMapper.getInventoryWarnings();
    }
}
