package com.github.gsmpro.brokerage.dto;

public record Quotation(
        double value,
        String link) {
    public static final Quotation DEFAULT = new Quotation(0, "https://www.google.com/");
    public static final Quotation YAHOO = new Quotation(0, "https://finance.yahoo.com/quote/");
}