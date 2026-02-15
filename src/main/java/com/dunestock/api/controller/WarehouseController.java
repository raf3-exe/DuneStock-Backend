package com.dunestock.api.controller;

import com.dunestock.api.repository.WarehousesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

public class WarehouseController {

    @Autowired
    private WarehousesRepository WarehousesRepository;

    public ResponseEntity<?> getWarehouses(@RequestBody Map<String, String> body) {
        return null;
    }
}
