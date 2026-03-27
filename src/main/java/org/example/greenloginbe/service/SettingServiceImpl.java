package org.example.greenloginbe.service;

import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.example.greenloginbe.entity.SystemSetting;
import org.example.greenloginbe.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SettingServiceImpl implements SettingService {

    private final SystemSettingRepository settingRepository;

    @Override
    public List<SystemSetting> getAllSettings() {
        return settingRepository.findAll();
    }

    @Override
    public List<SystemSetting> getSettingsByGroup(String groupName) {
        return settingRepository.findByGroupName(groupName);
    }

    @Override
    public Map<String, String> getSettingsByGroupAsMap(String groupName) {
        return getSettingsByGroup(groupName).stream()
                .collect(Collectors.toMap(SystemSetting::getKey, SystemSetting::getValue));
    }

    @Override
    @Transactional
    public void updateSettings(Map<String, String> settings) {
        settings.forEach((key, value) -> {
            settingRepository.findByKey(key).ifPresent(s -> {
                s.setValue(value);
                settingRepository.save(s);
            });
        });
    }

    @Override
    public String getSettingValue(String key, String defaultValue) {
        return settingRepository.findByKey(key)
                .map(SystemSetting::getValue)
                .orElse(defaultValue);
    }
}
