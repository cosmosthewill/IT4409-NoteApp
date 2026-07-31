package ziang.com.it4409.noteapp.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import ziang.com.it4409.noteapp.user.UserRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoDataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void initializationIsIdempotentWhenDemoUserExists() {
        when(userRepository.existsByUsernameIgnoreCase("demo")).thenReturn(true);
        DemoDataInitializer initializer = new DemoDataInitializer(
                userRepository,
                new BCryptPasswordEncoder(),
                "demo",
                "demo@example.com",
                "demo12345"
        );

        initializer.run(new DefaultApplicationArguments());

        verify(userRepository, never()).save(any());
    }
}
