package ziang.com.it4409.noteapp.auth;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import ziang.com.it4409.noteapp.auth.dto.RegistrationForm;
import ziang.com.it4409.noteapp.exception.DuplicateUserException;

@Controller
public class AuthController {

    private final RegistrationService registrationService;

    public AuthController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            return "redirect:/notes";
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Authentication authentication, Model model) {
        if (isAuthenticated(authentication)) {
            return "redirect:/notes";
        }
        if (!model.containsAttribute("registrationForm")) {
            model.addAttribute("registrationForm", new RegistrationForm());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registrationForm") RegistrationForm form,
            BindingResult bindingResult,
            HttpServletResponse response
    ) {
        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue(
                    "confirmPassword",
                    "validation.confirmPassword.mismatch",
                    "{validation.confirmPassword.mismatch}"
            );
        }

        if (bindingResult.hasErrors()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return "auth/register";
        }

        try {
            registrationService.register(form);
        } catch (DuplicateUserException exception) {
            if (exception.getField() == DuplicateUserException.Field.USERNAME) {
                bindingResult.rejectValue("username", "validation.username.duplicate");
            } else {
                bindingResult.rejectValue("email", "validation.email.duplicate");
            }
            response.setStatus(HttpStatus.CONFLICT.value());
            return "auth/register";
        }

        return "redirect:/login?registered";
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
