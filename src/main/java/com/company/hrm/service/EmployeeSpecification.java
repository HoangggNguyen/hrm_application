package com.company.hrm.service;

import com.company.hrm.entity.Employee;
import com.company.hrm.entity.EmployeeStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class EmployeeSpecification {

    private EmployeeSpecification() {
    }

    public static Specification<Employee> matching(String query, Long departmentId, Long positionId,
                                                    EmployeeStatus status, Long managerId) {
        return (root, criteria, builder) -> {
            var filters = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (query != null && !query.isBlank()) {
                String search = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                filters.add(builder.or(
                        builder.like(builder.lower(root.get("fullName")), search),
                        builder.like(builder.lower(root.get("code")), search),
                        builder.like(builder.lower(root.get("email")), search)));
            }
            if (departmentId != null) {
                filters.add(builder.equal(root.get("department").get("id"), departmentId));
            }
            if (positionId != null) {
                filters.add(builder.equal(root.get("position").get("id"), positionId));
            }
            if (status != null) {
                filters.add(builder.equal(root.get("status"), status));
            } else {
                filters.add(builder.notEqual(root.get("status"), EmployeeStatus.RESIGNED));
            }
            if (managerId != null) {
                filters.add(builder.equal(root.get("manager").get("id"), managerId));
            }
            return builder.and(filters.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}