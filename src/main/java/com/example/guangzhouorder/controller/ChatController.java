package com.example.guangzhouorder.controller;

import com.example.guangzhouorder.dto.ProductCardDto;
import com.example.guangzhouorder.entity.User;
import com.example.guangzhouorder.repository.ProductCardRepository;
import com.example.guangzhouorder.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final UserService userService;
    private final ProductCardRepository productCardRepository;

    @GetMapping("/chat")
    public String chat(@AuthenticationPrincipal UserDetails userDetails,
                       @RequestParam(required = false) Long cloneFrom,
                       Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);

        if (cloneFrom != null) {
            productCardRepository.findById(cloneFrom)
                    .filter(c -> c.isPublic())
                    .map(ProductCardDto::new)
                    .ifPresent(dto -> model.addAttribute("cloneSource", dto));
        }

        return "chat_with_customizer";
    }
}
