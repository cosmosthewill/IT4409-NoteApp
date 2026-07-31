package ziang.com.it4409.noteapp.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import ziang.com.it4409.noteapp.auth.dto.RegistrationForm;
import ziang.com.it4409.noteapp.exception.DuplicateUserException;
import ziang.com.it4409.noteapp.user.User;
import ziang.com.it4409.noteapp.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    private BCryptPasswordEncoder passwordEncoder;
    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        registrationService = new RegistrationService(userRepository, passwordEncoder);
    }

    @Test
    void rejectsDuplicateUsername() {
        RegistrationForm form = validForm();
        when(userRepository.existsByUsernameIgnoreCase("duy")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(form))
                .isInstanceOf(DuplicateUserException.class)
                .extracting("field")
                .isEqualTo(DuplicateUserException.Field.USERNAME);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDuplicateEmail() {
        RegistrationForm form = validForm();
        when(userRepository.existsByEmailIgnoreCase("duy@example.com")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(form))
                .isInstanceOf(DuplicateUserException.class)
                .extracting("field")
                .isEqualTo(DuplicateUserException.Field.EMAIL);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void storesPasswordAsBcryptHash() {
        RegistrationForm form = validForm();
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User saved = registrationService.register(form);

        assertThat(saved.getPasswordHash()).isNotEqualTo(form.getPassword());
        assertThat(saved.getPasswordHash()).startsWith("$2");
        assertThat(passwordEncoder.matches(form.getPassword(), saved.getPasswordHash())).isTrue();
    }

    private RegistrationForm validForm() {
        RegistrationForm form = new RegistrationForm();
        form.setUsername("duy");
        form.setEmail("duy@example.com");
        form.setPassword("password123");
        form.setConfirmPassword("password123");
        return form;
    }
}
