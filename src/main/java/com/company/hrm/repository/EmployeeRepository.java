package com.company.hrm.repository;

import com.company.hrm.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
	@Override
	@EntityGraph(attributePaths = {"department", "position", "manager"})
	Page<Employee> findAll(Specification<Employee> specification, Pageable pageable);

	@Override
	@EntityGraph(attributePaths = {"department", "position", "manager"})
	java.util.List<Employee> findAll(Specification<Employee> specification, org.springframework.data.domain.Sort sort);

	@EntityGraph(attributePaths = {"department", "manager"})
	java.util.List<Employee> findAllByOrderByCode();

	boolean existsByEmail(String email);
	boolean existsByIdNumber(String idNumber);
	boolean existsByDepartment_Id(Long departmentId);
	boolean existsByPosition_Id(Long positionId);
	List<Employee> findByManager_IdOrderByCode(Long managerId);

	@Query("select count(e) from Employee e")
	long countEmployees();

	@Query("select e from Employee e left join fetch e.department left join fetch e.position left join fetch e.manager where e.id = :id")
	java.util.Optional<Employee> findWithReferencesById(@Param("id") Long id);
}