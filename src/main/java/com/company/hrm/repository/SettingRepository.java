package com.company.hrm.repository;

import com.company.hrm.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettingRepository extends JpaRepository<Setting, Long> {
    Optional<Setting> findBySettingKey(String settingKey);
}