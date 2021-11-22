package com.github.gsmpro.brokerage.dto;

import java.util.List;

public record Generated(
        List<List<String>> rows,
        String firstDealDescription,
        String lastDealDescription,
        String reportDate) {
}