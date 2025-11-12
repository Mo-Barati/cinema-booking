package com.cinema.service;

import com.cinema.entity.Cinema;
import com.cinema.exception.ResourceNotFoundException;
import com.cinema.repository.CinemaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CinemaService {

    private final CinemaRepository cinemaRepository;

    public CinemaService(CinemaRepository cinemaRepository) {
        this.cinemaRepository = cinemaRepository;
    }

    public List<Cinema> getAllCinemas() {
        return cinemaRepository.findAll();
    }

    public Cinema createCinema(Cinema cinema) {
        return cinemaRepository.save(cinema);
    }

    public void deleteCinema(Long id) {
        if (!cinemaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cinema not found with id: " + id);
        }
        cinemaRepository.deleteById(id);
    }

    public Cinema updateCinema(Long id, Cinema cinema) {
        // If cinema does not exist, fail early (optional but safer)
        Cinema existing = cinemaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cinema not found with id " + id));

        // Make sure we update the correct record
        cinema.setId(existing.getId());

        // Save the full entity (your frontend sends all fields)
        return cinemaRepository.save(cinema);
    }

}
