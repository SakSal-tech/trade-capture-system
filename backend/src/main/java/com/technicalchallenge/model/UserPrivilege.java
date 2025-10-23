package com.technicalchallenge.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_privilege")
@IdClass(UserPrivilegeId.class)
public class UserPrivilege {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "privilege_id")
    private Long privilegeId;

    // Relationship to ApplicationUser (no FK enforcement at JPA level)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id", insertable = false, updatable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ApplicationUser user;

    // Relationship to Privilege (no FK enforcement at JPA level)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "privilege_id", insertable = false, updatable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Privilege privilege;
}
