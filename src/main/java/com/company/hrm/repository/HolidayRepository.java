package com.company.hrm.repository;

import com.company.hrm.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    boolean existsByHolidayDate(LocalDate holidayDate);
    List<Holiday> findByHolidayDateBetween(LocalDate from, LocalDate to);
    List<Holiday> findAllByOrderByHolidayDate();
}