package com.oversight.issuance.domain;

import java.util.Objects;
import java.util.Set;

/** An authenticated person. A record gives equals/hashCode/toString for free and is immutable. */
public record User(String id, String name, Set<Role> roles) {

    public User {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        roles = Set.copyOf(roles); // defensive copy: caller can't change our roles later
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }
}
