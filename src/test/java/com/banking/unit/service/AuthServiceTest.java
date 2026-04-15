package com.banking.unit.service;

import com.banking.dto.auth.LoginRequest;
import com.banking.dto.auth.LoginResponse;
import com.banking.dto.auth.RegisterRequest;
import com.banking.dto.user.UserDTO;
import com.banking.entity.User;
import com.banking.enums.UserRole;
import com.banking.exception.BadRequestException;
import com.banking.exception.UnauthorizedException;
import com.banking.repository.UserRepository;
import com.banking.security.JwtTokenProvider;
import com.banking.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private User existingUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setPassword("Password1!");
        registerRequest.setFirstName("New");
        registerRequest.setLastName("User");
        registerRequest.setEmail("new@example.nl");
        registerRequest.setPhoneNumber("+31612345678");
        registerRequest.setBsn("111222333");
        registerRequest.setDateOfBirth(LocalDate.of(1995, 1, 1));

        existingUser = User.builder()
            .id(1L)
            .username("johndoe")
            .password("encodedPassword")
            .firstName("John")
            .lastName("Doe")
            .email("john@example.nl")
            .phoneNumber("+31698765432")
            .bsn("987654321")
            .dateOfBirth(LocalDate.of(1990, 6, 20))
            .role(UserRole.CUSTOMER)
            .approved(true)
            .build();
    }

    @Test
    void register_success() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.nl")).thenReturn(false);
        when(userRepository.existsByBsn("111222333")).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(5L);
            return u;
        });

        UserDTO result = authService.register(registerRequest);

        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.isApproved()).isFalse();
        assertThat(result.getRole()).isEqualTo(UserRole.CUSTOMER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateUsername_throws() {
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Username already taken");
    }

    @Test
    void register_duplicateEmail_throws() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.nl")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Email already in use");
    }

    @Test
    void login_success() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("johndoe");
        loginRequest.setPassword("Password1!");

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("Password1!", "encodedPassword")).thenReturn(true);
        when(tokenProvider.generateToken("johndoe")).thenReturn("jwt-token-here");

        LoginResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token-here");
        assertThat(response.getUser().getUsername()).isEqualTo("johndoe");
    }

    @Test
    void login_wrongPassword_throws() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("johndoe");
        loginRequest.setPassword("WrongPassword");

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("WrongPassword", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Invalid credentials");
    }

    @Test
    void login_userNotFound_throws() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("unknown");
        loginRequest.setPassword("any");

        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
            .isInstanceOf(UnauthorizedException.class);
    }
}
