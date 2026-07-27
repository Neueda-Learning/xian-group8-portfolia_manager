package com.example.costomerservice.controller;

import com.example.costomerservice.custormer.Custormer;
import com.example.costomerservice.repo.CustormerRepo;
import org.springframework.web.bind.annotation.*;

@RestController
public class CustormerController {
    private final CustormerRepo repository;

    public CustormerController(CustormerRepo repository) {
        this.repository = repository;
    }

    @GetMapping("/custormer/{id}")
    public Custormer findCustormerById(@PathVariable int id) {
        return repository.findCustormers(id);
    }

    @PostMapping("/save")
    public String saveCustormer(@RequestBody Custormer custormer) {
        int result = repository.save(custormer);
        if (result == 1) {
            return "Custormer saved successfully.";
        } else {
            return "Failed to save custormer.";
        }
    }
}
