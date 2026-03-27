package org.example.greenloginbe.controller;

import lombok.RequiredArgsConstructor;
import org.example.greenloginbe.entity.SystemSetting;
import org.example.greenloginbe.service.SettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;

    @GetMapping
    public ResponseEntity<List<SystemSetting>> getAll() {
        return ResponseEntity.ok(settingService.getAllSettings());
    }

    @GetMapping("/group/{groupName}")
    public ResponseEntity<List<SystemSetting>> getByGroup(@PathVariable String groupName) {
        return ResponseEntity.ok(settingService.getSettingsByGroup(groupName));
    }

    @PostMapping("/update")
    public ResponseEntity<String> update(@RequestBody Map<String, String> settings) {
        settingService.updateSettings(settings);
        return ResponseEntity.ok("Settings updated successfully");
    }
}
