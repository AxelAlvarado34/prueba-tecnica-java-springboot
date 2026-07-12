package com.axel.alvarado.coworking_service;

import com.axel.alvarado.coworking_service.dto.ReservationRequest;
import com.axel.alvarado.coworking_service.dto.SpaceRequest;
import com.axel.alvarado.coworking_service.enums.SpaceType;
import com.axel.alvarado.coworking_service.service.PaymentGatewayClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private PaymentGatewayClient paymentGatewayClient;

        @Test
        @WithMockUser(username = "admin@coworking.com", roles = "ADMIN")
        void createReservation_thenRejectOverlapping() throws Exception {
                Mockito.when(paymentGatewayClient.validatePayment(Mockito.anyLong())).thenReturn(true);

                SpaceRequest spaceRequest = new SpaceRequest("Sala Test", SpaceType.MEETING_ROOM, 4, "Piso 1",
                                BigDecimal.TEN);
                String spaceJson = mockMvc.perform(post("/api/spaces")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(spaceRequest)))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();

                Long spaceId = objectMapper.readTree(spaceJson).get("id").asLong();

                ReservationRequest reservationRequest = new ReservationRequest(
                                spaceId, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2));

                mockMvc.perform(post("/api/reservations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(reservationRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.status").value("CONFIRMED"));

                mockMvc.perform(post("/api/reservations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(reservationRequest)))
                                .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(username = "user@test.com", roles = "USER")
        void getAllReservations_whenNotAdmin_returnsForbidden() throws Exception {
                mockMvc.perform(get("/api/reservations"))
                                .andExpect(status().isForbidden());
        }
}