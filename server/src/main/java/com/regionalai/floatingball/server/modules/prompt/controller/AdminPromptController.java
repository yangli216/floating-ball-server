package com.regionalai.floatingball.server.modules.prompt.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.prompt.entity.AiPrompt;
import com.regionalai.floatingball.server.modules.prompt.service.PromptService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/admin/api/prompts")
public class AdminPromptController {

    private final PromptService promptService;

    public AdminPromptController(PromptService promptService) {
        this.promptService = promptService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AiPrompt>> list(@RequestParam(defaultValue = "1") long current,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) String keyword,
                                                    HttpServletRequest request) {
        return ApiResponse.success(promptService.list(current, size, keyword), RequestIdUtils.resolve(request));
    }

    @PostMapping
    public ApiResponse<AiPrompt> save(@RequestBody AiPrompt prompt, HttpServletRequest request) {
        return ApiResponse.success(promptService.save(prompt), RequestIdUtils.resolve(request));
    }

    @PutMapping("/{idPrompt}")
    public ApiResponse<AiPrompt> update(@PathVariable String idPrompt,
                                        @RequestBody AiPrompt prompt,
                                        HttpServletRequest request) {
        return ApiResponse.success(promptService.update(idPrompt, prompt), RequestIdUtils.resolve(request));
    }

    @PostMapping("/{idPrompt}/publish")
    public ApiResponse<Void> publish(@PathVariable String idPrompt, HttpServletRequest request) {
        promptService.publish(idPrompt);
        return ApiResponse.success(null, RequestIdUtils.resolve(request));
    }

    @PostMapping("/{idPrompt}/archive")
    public ApiResponse<Void> archive(@PathVariable String idPrompt, HttpServletRequest request) {
        promptService.archive(idPrompt);
        return ApiResponse.success(null, RequestIdUtils.resolve(request));
    }

    @DeleteMapping("/{idPrompt}")
    public ApiResponse<Void> invalidate(@PathVariable String idPrompt, HttpServletRequest request) {
        promptService.invalidate(idPrompt);
        return ApiResponse.success(null, RequestIdUtils.resolve(request));
    }
}
