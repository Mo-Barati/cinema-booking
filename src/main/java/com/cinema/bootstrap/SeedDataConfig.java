package com.cinema.bootstrap;

import com.cinema.entity.Cinema;
import com.cinema.entity.Showtime;
import com.cinema.repository.CinemaRepository;
import com.cinema.repository.ShowtimeRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;

/**
 * Seeds a couple of Cinemas + Showtimes at app startup (only if DB is empty).
 * Safe to re-run: it checks counts before inserting.
 */
@Configuration
public class SeedDataConfig {

    @Bean
    CommandLineRunner seedData(CinemaRepository cinemaRepo, ShowtimeRepository showtimeRepo) {
        return args -> {
            if (cinemaRepo.count() > 0 || showtimeRepo.count() > 0) {
                return; // already seeded
            }

            // TODO: Adjust these setters/fields to match your Cinema entity names.
            // Minimal example with typical fields:
            Cinema odeon = new Cinema();
            // odeon.setName("Odeon Leicester Square");
            // odeon.setAddress("24-26 Leicester Square");
            // odeon.setCity("London");
            // odeon.setPostcode("WC2H 7JY");
            // odeon.setPhone("+44 20 1234 5678");
            // odeon.setEmail("leicester@odeon.co.uk");

            Cinema imax = new Cinema();
            // imax.setName("BFI IMAX Waterloo");
            // imax.setAddress("1 Charlie Chaplin Walk");
            // imax.setCity("London");
            // imax.setPostcode("SE1 8XR");
            // imax.setPhone("+44 20 8765 4321");
            // imax.setEmail("boxoffice@bfi-imax.co.uk");

            odeon = cinemaRepo.save(odeon);
            imax  = cinemaRepo.save(imax);

            // Showtimes (use your fields exactly as in Showtime.java)
            Showtime s1 = new Showtime(
                    "The Batman",
                    1,
                    LocalDateTime.now().plusDays(1).withHour(18).withMinute(30).withSecond(0).withNano(0),
                    LocalDateTime.now().plusDays(1).withHour(21).withMinute(0).withSecond(0).withNano(0),
                    12.50,
                    "English",
                    "2D",
                    odeon
            );

            Showtime s2 = new Showtime(
                    "Dune: Part Two",
                    2,
                    LocalDateTime.now().plusDays(1).withHour(20).withMinute(0).withSecond(0).withNano(0),
                    LocalDateTime.now().plusDays(1).withHour(23).withMinute(0).withSecond(0).withNano(0),
                    15.00,
                    "English",
                    "IMAX 3D",
                    imax
            );

            showtimeRepo.save(s1);
            showtimeRepo.save(s2);

            // Optional: more samples
            Showtime s3 = new Showtime(
                    "Inside Out 2",
                    3,
                    LocalDateTime.now().plusDays(2).withHour(14).withMinute(0).withSecond(0).withNano(0),
                    LocalDateTime.now().plusDays(2).withHour(16).withMinute(0).withSecond(0).withNano(0),
                    9.99,
                    "English",
                    "2D",
                    odeon
            );
            showtimeRepo.save(s3);
        };
    }

}
