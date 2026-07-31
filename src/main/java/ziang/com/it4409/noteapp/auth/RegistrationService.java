package ziang.com.it4409.noteapp.auth;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ziang.com.it4409.noteapp.auth.dto.RegistrationForm;
import ziang.com.it4409.noteapp.exception.DuplicateUserException;
import ziang.com.it4409.noteapp.user.User;
import ziang.com.it4409.noteapp.user.UserRepository;

import java.util.Locale;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegistrationForm form) {
        String username = form.getUsername().trim().toLowerCase(Locale.ROOT);
        String email = form.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateUserException(DuplicateUserException.Field.USERNAME);
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateUserException(DuplicateUserException.Field.EMAIL);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));

        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            if (userRepository.existsByUsernameIgnoreCase(username)) {
                throw new DuplicateUserException(DuplicateUserException.Field.USERNAME);
            }
            throw new DuplicateUserException(DuplicateUserException.Field.EMAIL);
        }
    }
}
