package com.carbuyer.bot.controller;

import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.bot.service.BotDistributeService;
import com.carbuyer.exception.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/v1/bot")
@RequiredArgsConstructor
@Tag(name = "Bot", description = "Endpoints for bot management")
public class BotController {

    private final BotDistributeService service;

    @PostMapping("/add")
    @Operation(
            summary = "Enabled bots in system",
            description = "Enabled existed bots, and start his infinity cycle"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Bots successfully enabled",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StartBotResponse.class)
                    )),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    ))
    })
    public ResponseEntity<StartBotResponse> addBots() {
        StartBotResponse response = service.botAdderManager();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
