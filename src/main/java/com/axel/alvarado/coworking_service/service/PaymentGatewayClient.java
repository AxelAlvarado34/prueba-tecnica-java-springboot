package com.axel.alvarado.coworking_service.service;

import com.axel.alvarado.coworking_service.config.PaymentGatewayProperties;
import com.axel.alvarado.coworking_service.exception.PaymentGatewayException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentGatewayClient {

    private final RestClient paymentRestClient;
    private final PaymentGatewayProperties properties;

    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "fallback")
    public boolean validatePayment(Long reservationId) {
        try {
            paymentRestClient.post()
                    .uri(properties.getUrl())
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException ex) {
            throw new PaymentGatewayException("Fallo al validar el pago de la reserva " + reservationId, ex);
        }
    }

    private boolean fallback(Long reservationId, Throwable t) {
        log.warn("Circuit breaker activo para reserva {}: {}", reservationId, t.getMessage());
        return false;
    }
}