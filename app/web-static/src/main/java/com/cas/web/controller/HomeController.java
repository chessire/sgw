package com.cas.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;

/**
 * 홈 컨트롤러
 */
@Slf4j
@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        log.debug("Home page requested");
        model.addAttribute("title", "Welcome");
        model.addAttribute("message", "Spring Framework 4.3.30 + Thymeleaf 기반 웹 애플리케이션");
        model.addAttribute("timestamp", LocalDateTime.now());
        return "home";
    }

    @GetMapping("/about")
    public String about(Model model) {
        log.debug("About page requested");
        model.addAttribute("title", "About");
        model.addAttribute("message", "멀티모듈 Maven 프로젝트");
        return "about";
    }

    /**
     * Cocostudio 통합 예시 페이지
     */
    @GetMapping("/cocostudio-example")
    public String cocostudioExample(Model model) {
        log.debug("Cocostudio example page requested");
        
        // Cocostudio HTML에 주입할 동적 데이터
        model.addAttribute("username", "김철수");
        model.addAttribute("email", "kim.chulsoo@example.com");
        model.addAttribute("apiEndpoint", "http://localhost:8080/api");
        model.addAttribute("encryptedSecretKey", "AES256_ENCRYPTED_KEY_SAMPLE_12345");
        model.addAttribute("userId", "USER_67890");
        
        return "cocostudio-example";
    }
}

