package com.example.springai.controller;

import com.example.springai.domain.openai.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * RestController를 사용하지 않고 Controller를 사용한 이유론
 * Get방식의 chatPage메소드 안에 return "chat"이
 * ResetController는 실제 문자열 chat을 반환하며 설정해놓은 chat.mustach파일을 찾아 랜더링 하지 않음
 * 고로 Controller 어노테이션을 사용 후 다른 메소드엔 ResponseBody 어노테이션 사용
 */
@Controller
@RequiredArgsConstructor
public class ChatController {
    private final OpenAIService openAIService;

    
    @PostMapping("/chat")
    @ResponseBody
    public String chat(@RequestBody Map<String, String> body) {
        return openAIService.generate(body.get("text"));
    }


    @PostMapping("/chat/stream")
    @ResponseBody
    public Flux<String> streamChat(@RequestBody Map<String, String> body) {
        return openAIService.generateStream(body.get("text"));
    }

    @GetMapping("/")
    public String chatPage() {
        return "chat";
    }
}
