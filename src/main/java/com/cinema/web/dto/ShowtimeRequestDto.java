package com.cinema.web.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public class ShowtimeRequestDto {

    @NotBlank
    @Size(max = 150)
    public String movieTitle;

    @Positive
    public int screenNumber;

    @NotNull
    public LocalDateTime startTime;

    @NotNull
    public LocalDateTime endTime;

    @DecimalMin(value = "0.0")
    public double ticketPrice;

    @Size(max = 50)
    public String language;

    @Size(max = 50)
    public String format;

    @NotNull
    public Long cinemaId;
}
