package com.technicalchallenge.repository;

import com.technicalchallenge.model.UserPrivilege;
import com.technicalchallenge.model.UserPrivilegeId;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPrivilegeRepository extends JpaRepository<UserPrivilege, UserPrivilegeId> {
    // Find all privilege links for a user by their loginId (case-insensitive)
    List<UserPrivilege> findByUser_LoginIdIgnoreCase(String loginId);

    // Find privilege links for a specific user and privilege name
    // (case-insensitive)
    List<UserPrivilege> findByUser_LoginIdIgnoreCaseAndPrivilege_NameIgnoreCase(String loginId, String privilegeName);
}
