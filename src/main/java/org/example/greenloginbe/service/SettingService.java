package org.example.greenloginbe.service;

import org.example.greenloginbe.entity.SystemSetting;
import java.util.List;
import java.util.Map;

public interface SettingService {
    List<SystemSetting> getAllSettings();
    List<SystemSetting> getSettingsByGroup(String groupName);
    Map<String, String> getSettingsByGroupAsMap(String groupName);
    void updateSettings(Map<String, String> settings);
    String getSettingValue(String key, String defaultValue);
}
