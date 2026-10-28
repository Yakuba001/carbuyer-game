package com.carbuyer.bot.controller;

import com.carbuyer.bot.dto.StartBotRequest;
import com.carbuyer.containers.MockMvcIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
public class BotControllerIT extends MockMvcIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(value = "bib@g.c", roles = "ADMIN")
    void botsOn_shouldReturn201_whenRequestIsCorrectAndBotsWereCreated() throws Exception {
        StartBotRequest request = new StartBotRequest(100);

        mockMvc.perform(post("/api/v1/bot/on")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @SuppressWarnings("all")
    @WithMockUser(value = "bib@g.c", roles = "ADMIN")
    void botsOn_shouldReturn400_whenRequestInvalid() throws Exception {
        StartBotRequest request = new StartBotRequest(-1);

        mockMvc.perform(post("/api/v1/bot/on")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(value = "bib@g.c", roles = "USER")
    void botsOn_shouldReturn403_whenAccessForbidden() throws Exception {
        StartBotRequest request = new StartBotRequest(100);

        mockMvc.perform(post("/api/v1/bot/on")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(value = "bib@g.c", roles = "ADMIN")
    void botsOff_shouldReturn200_whenThreadPoolWithBotsShutDowns() throws Exception {
        mockMvc.perform(post("/api/v1/bot/off"))
                .andExpect(status().isOk());
    }

    @Test
    void botsOff_shouldReturn403_whenAccessForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/bot/off"))
                .andExpect(status().isForbidden());
    }
}
