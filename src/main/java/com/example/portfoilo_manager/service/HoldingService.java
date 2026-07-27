package com.example.portfoilo_manager.service;

import com.example.portfoilo_manager.model.Holding;
import com.example.portfoilo_manager.repository.HoldingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HoldingService {

    private final HoldingRepository repository;

    public HoldingService(HoldingRepository repository) {
        this.repository = repository;
    }

    public List<Holding> getAllHoldings() {
        return repository.findAll();
    }

    public Holding getHoldingById(int id) {
        return repository.findById(id);
    }

    public Holding addHolding(Holding holding) {
        int id = repository.save(holding);
        holding.setId(id);
        return holding;
    }

    public boolean deleteHolding(int id) {
        return repository.deleteById(id) > 0;
    }
}

