package com.example.springai.controller;

import com.example.springai.domain.openai.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ChatController {
    private final OpenAIService openAIService;

    
    @PostMapping("/chat")
    public String chat(@RequestBody Map<String, String> body) {
        return openAIService.generate(body.get("text"));
    }


    @PostMapping("/chat/stream")
    public Flux<String> streamChat(@RequestBody Map<String, String> body) {
        return openAIService.generateStream(body.get("text"));
    }

    @GetMapping("/")
    public String chatPage() {
        return "chat";
    }
}
