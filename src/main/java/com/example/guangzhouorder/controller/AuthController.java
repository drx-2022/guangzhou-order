package com.example.guangzhouorder.controller;

import com.example.guangzhouorder.dto.auth.SignUpRequest;
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
        return "auth/sign_up";
    }

    @PostMapping("/signup")
    public String processSignUp(@Valid @ModelAttribute("signUpRequest") SignUpRequest request,
                                BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/sign_up";
        }
        try {
            String email = request.getEmail();
            userService.registerUser(request.getName(), email,
                    request.getPhone(), request.getPassword(), request.getRole());
            
            // Add email and masked email to model for verify_pending page
            model.addAttribute("email", email);
            model.addAttribute("maskedEmail", maskEmail(email));
            return "verify_pending";
        } catch (IllegalArgumentException e) {
            model.addAttribute("signupError", e.getMessage());
            return "auth/sign_up";
        }
    }

    @PostMapping("/resend-verification")
    public String resendVerificationEmail(@RequestParam String email, Model model) {
        try {
            userService.resendVerificationEmail(email);
            model.addAttribute("email", email);
            model.addAttribute("maskedEmail", maskEmail(email));
            model.addAttribute("resendSuccess", true);
            return "verify_pending";
        } catch (IllegalArgumentException e) {
            model.addAttribute("email", email);
            model.addAttribute("maskedEmail", maskEmail(email));
            model.addAttribute("resendError", e.getMessage());
            return "verify_pending";
        }
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token, Model model) {
        try {
            userService.verifyEmail(token);
            return "redirect:/login?verified=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "login";
        }
    }

    @GetMapping("/login")
    public String showLogin(@RequestParam(required = false) String registered,
                           @RequestParam(required = false) String verified,
                           @RequestParam(required = false) String error,
                           Model model) {

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

    
        if (registered != null) {
            model.addAttribute("successMessage", "Account created! Please log in.");
        }
        if (verified != null) {
            model.addAttribute("verifiedMessage", "Your email has been verified. Please log in.");
        }
        if ("unverified".equals(error)) {
            model.addAttribute("loginError", "Please verify your email before logging in.");
        } else if ("invalid-token".equals(error)) {
            model.addAttribute("loginError", "Invalid or expired verification token.");
        }
        return "login";
    

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
            if (!user.isAccountVerified()) {
                model.addAttribute("loginError", "Please verify your email before logging in.");
                return "redirect:/login?error=unverified";
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
            return "auth/login";
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
