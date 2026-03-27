package org.example.greenloginbe.repository;

import org.example.greenloginbe.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, Integer> {
    Optional<SystemSetting> findByKey(String key);
    List<SystemSetting> findByGroupName(String groupName);
}
