package com.github.gsmpro.brokerage.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "report")
@Getter
@Setter
public class ReportConfig {
    private String template;
    private String interval;
    private String styles;
    private String filename;
    private String headerLayoutFile;
    private String columnsOpenedLayoutFile;
    private String columnsClosedLayoutFile;
    private String summaryLayoutFile;
    private List<String> allowedPlaces;
    private List<String> tickers;
    private List<String> rawQuotationSources;
    private Boolean useRawQuotationSources;
}