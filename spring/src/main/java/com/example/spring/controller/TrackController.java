package com.example.spring.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring.dto.TrackDto;
import com.example.spring.service.TrackService;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TrackController {

    private final TrackService trackService;

    public TrackController(TrackService trackService) {
        this.trackService = trackService;
    }

    @GetMapping("/tracks")
    public ResponseEntity<List<TrackDto>> getTracks() {
        List<TrackDto> trackList = trackService.getTracks();
        return ResponseEntity.ok(trackList);    // 200
    }
}
