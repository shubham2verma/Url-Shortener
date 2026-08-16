package UrlShortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import UrlShortener.exception.InvalidRoleException;
import UrlShortener.exception.UserNotFoundException;
import UrlShortener.model.User;
import UrlShortener.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    @DisplayName("updateUserRole: valid input saves user with new role")
    void updateUserRole_withValidInput_savesUserWithNewRole() {
        User user = new User();
        user.setUsername("alice");
        user.setRole("ROLE_READ");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        adminService.updateUserRole("alice", "ROLE_WRITE");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo("ROLE_WRITE");
    }

    @Test
    @DisplayName("updateUserRole: invalid role throws InvalidRoleException")
    void updateUserRole_withInvalidRole_throwsInvalidRoleException() {
        assertThatThrownBy(() -> adminService.updateUserRole("alice", "ROLE_SUPERUSER"))
                .isInstanceOf(InvalidRoleException.class)
                .hasMessageContaining("ROLE_SUPERUSER")
                .hasMessageContaining("Allowed:");

        verify(userRepository, never()).findByUsername(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUserRole: unknown username throws UserNotFoundException")
    void updateUserRole_withUnknownUsername_throwsUserNotFoundException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateUserRole("ghost", "ROLE_WRITE"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("ghost");

        verify(userRepository, never()).save(any());
    }
}
