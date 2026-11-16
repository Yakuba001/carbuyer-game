package com.carbuyer.bot.controller;

import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.bot.dto.StopBotResponse;
import com.carbuyer.bot.service.BotService;
import com.carbuyer.exception.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/v1/bot")
@Validated
@RequiredArgsConstructor
@Tag(name = "Bot", description = "Endpoints for bot management")
public class BotController {

    private final BotService service;

    @PostMapping("/start/{amount}")
    @Operation(
            summary = "Start worker threads",
            description = "Enabled existed bots, added amount of worker threads and start lifecycle"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Workers successfully enabled",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StartBotResponse.class)
                    )),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
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
                    )),
            @ApiResponse(responseCode = "500", description = "Unexpected error occurred",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    ))
    })
    public ResponseEntity<StartBotResponse> addBots(@Min(1) @Max(999) @PathVariable int amount) {
        StartBotResponse response = service.startBots(amount);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/stop")
    @Operation(
            summary = "Stop worker threads",
            description = "Stopping all bots processes"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Workers successfully unabled",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StopBotResponse.class)
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
    public ResponseEntity<StopBotResponse> stopBots() {
        StopBotResponse response = service.stopBots();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}
