package com.carbuyer.user.api;

import org.springframework.security.core.userdetails.UserDetails;

public interface UserFacade {

    boolean existByEmail(String email);
    boolean existByUsername(String username);
    UserDetails save(String username, String email, String password);
    UserDetails findByEmail(String email);
    Long findIdByUsername(String username);
}
