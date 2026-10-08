package com.carbuyer.user.internal;

import com.carbuyer.user.api.UserFacade;
import com.carbuyer.user.internal.entity.User;
import com.carbuyer.user.internal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserFacadeImpl implements UserFacade {

    private final UserRepository userRepository;

    @Override
    public boolean existByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean existByUsername(String username) {
        return userRepository.existsByName(username);
    }

    @Override
    public User save(String username, String email, String password) {
        return userRepository.save(new User(username, email, password));
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User with this email does not exists"));
    }
}
