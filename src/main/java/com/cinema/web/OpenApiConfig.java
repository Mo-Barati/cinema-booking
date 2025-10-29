package com.cinema.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Cinema Ticket Booking API",
                version = "v1",
                description = "Showtimes and Cinemas endpoints"
        )
)
public class OpenApiConfig {}
