package com.springliviu.ivantask.controller;

import com.springliviu.ivantask.service.GridService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class GridController {

    private final GridService gridService;

    public GridController(GridService gridService) {
        this.gridService = gridService;
    }

    @GetMapping("/api/generate")
    public Map<String, Object> generateFigure() {
        return gridService.generateFigure();
    }
}
