package com.example.guangzhouorder.controller;

import com.example.guangzhouorder.dto.SignUpRequest;
import com.example.guangzhouorder.entity.User;
import com.example.guangzhouorder.security.JwtTokenProvider;
import com.example.guangzhouorder.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/signup")
    public String showSignUp(Model model) {
        model.addAttribute("signUpRequest", new SignUpRequest());
        return "sign_up";
    }

    @PostMapping("/signup")
    public String processSignUp(@Valid @ModelAttribute("signUpRequest") SignUpRequest request,
                                BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "sign_up";
        }
        try {
            userService.registerUser(request.getName(), request.getEmail(),
                    request.getPhone(), request.getPassword(), request.getRole());
        } catch (IllegalArgumentException e) {
            model.addAttribute("signupError", e.getMessage());
            return "sign_up";
        }
        return "redirect:/login?registered=true";
    }

    @GetMapping("/login")

    public String loginPage(Model model) {
        // Get the current authentication context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Check if the user is already logged in (and not an anonymous guest)
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            // Redirect them away from the login page
            return "redirect:/dashboard";
        }

        // If they are not logged in, show the login form normally
        model.addAttribute("pageTitle", "Login | Guangzhou Direct");
        return "login";
    }

    public String showLogin(@RequestParam(required = false) String registered, Model model) {
        if (registered != null) {
            model.addAttribute("successMessage", "Account created! Please log in.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String email,
                               @RequestParam String password,
                               HttpServletResponse response,
                               Model model) {
        try {
            User user = userService.findByEmail(email);
            if (!passwordEncoder.matches(password, user.getHashedPassword())) {
                model.addAttribute("loginError", "Invalid email or password.");
                return "login";
            }
            String token = jwtTokenProvider.generateToken(email);
            Cookie cookie = new Cookie("jwt", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(86400);
            response.addCookie(cookie);
            return "redirect:/dashboard";
        } catch (IllegalArgumentException e) {
            model.addAttribute("loginError", "Invalid email or password.");
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/";
    }
}
