package com.company.hrm.repository;

import com.company.hrm.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PositionRepository extends JpaRepository<Position, Long> {
	List<Position> findAllByOrderByName();
	boolean existsByNameIgnoreCase(String name);
}