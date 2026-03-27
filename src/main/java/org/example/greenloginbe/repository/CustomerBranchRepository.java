package org.example.greenloginbe.repository;

import org.example.greenloginbe.entity.CustomerBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerBranchRepository extends JpaRepository<CustomerBranch, Integer> {
}
