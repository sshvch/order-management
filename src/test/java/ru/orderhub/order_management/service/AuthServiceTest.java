package ru.orderhub.order_management.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.orderhub.order_management.exception.BadRequestException;
import ru.orderhub.order_management.security.JwtService;
import ru.orderhub.order_management.dto.AuthResponse;
import ru.orderhub.order_management.dto.RegisterRequest;
import ru.orderhub.order_management.entity.Role;
import ru.orderhub.order_management.entity.User;
import ru.orderhub.order_management.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCreatesUserWithHashedPassword() {
        when(userRepository.existsByEmail("test@orderhub.local")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);

        AuthResponse response = authService.register(
                new RegisterRequest("Test@OrderHub.Local", "secret123")
        );

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("test@orderhub.local");
        assertThat(response.role()).isEqualTo("USER");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();

        assertThat(savedUser.getPassword()).isEqualTo("hashed-password");
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void registerThrowsWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("test@orderhub.local")).thenReturn(true);

        assertThatThrownBy(() ->
                authService.register(new RegisterRequest("test@orderhub.local", "secret123"))
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("уже существует");

        verify(userRepository, never()).save(any(User.class));
    }
}
