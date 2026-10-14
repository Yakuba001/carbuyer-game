package com.carbuyer.user.internal;

import com.carbuyer.user.api.UserFacade;
import com.carbuyer.user.api.dto.CreateUserCommand;
import com.carbuyer.user.api.dto.UserDto;
import com.carbuyer.user.internal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserFacadeImpl implements UserFacade {

    private final UserService service;

    @Override
    public boolean existByEmail(String email) {
        return service.isUserExistsByEmail(email);
    }

    @Override
    public boolean existByUsername(String username) {
        return service.isUserExistsByName(username);
    }

    @Override
    public UserDetails save(String username, String email, String password) {
        UserDto user = service.createUser(new CreateUserCommand(username, email, password));
        return User.builder().username(user.name()).build();
    }

    @Override
    public UserDetails findByEmail(String email) {
        return User.builder().username(service.getUsernameByEmail(email)).build();
    }

    @Override
    public Long findIdByUsername(String username) {
        return service.findIdByUsername(username);
    }
}
