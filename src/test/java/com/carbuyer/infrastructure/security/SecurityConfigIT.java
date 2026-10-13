package com.carbuyer.infrastructure.security;

import com.carbuyer.infrastructure.security.controller.AuthController;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
public class SecurityConfigIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private AuthController controller;

    @TestConfiguration
    static class TestControllerConfig {
        @RestController
        static class TestAuthDeclarationController {
            @GetMapping("/api/v1/auth/test")
            public String authTest() {
                return "public_ok";
            }
        }

        @RestController
        static class TestProtectedDeclarationController {
            @GetMapping("/api/v1/protected/test")
            public String protectedTest() {
                return "protected_ok";
            }
        }
    }

    // TODO: Rebuild this..........................

//    @Test
//    void publicEndpoint_withoutToken_shouldReturn200() throws Exception {
//        mockMvc.perform(get("/api/v1/auth/test"))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    void protectedEndpoint_withoutToken_shouldReturn401() throws Exception {
//        mockMvc.perform(get("/api/v1/protected/test"))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    void protectedEndpoint_withValidToken_shouldReturn200() throws Exception {
//        String token = "valid_token";
//        String email = "test@mail.com";
//        UserDetails userDetails = new User(email, "password", Collections.emptyList());
//
//        when(jwtService.extractUsername(token)).thenReturn(email);
//        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
//        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);
//
//        mockMvc.perform(get("/api/v1/protected/test")
//                        .header("Authorization", "Bearer " + token))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    void protectedEndpoint_withInvalidToken_shouldReturn401() throws Exception {
//        String token = "invalid_token";
//
//        when(jwtService.extractUsername(token)).thenThrow(new MalformedJwtException("Invalid token"));
//
//        mockMvc.perform(get("/api/v1/protected/test")
//                        .header("Authorization", "Bearer " + token))
//                .andExpect(status().isForbidden());
//    }
}
