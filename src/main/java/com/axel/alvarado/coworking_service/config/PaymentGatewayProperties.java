package com.axel.alvarado.coworking_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "payment.gateway")
public class PaymentGatewayProperties {
    private String url;
}