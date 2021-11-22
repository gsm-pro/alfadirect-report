package com.github.gsmpro.brokerage.service;

import com.github.gsmpro.brokerage.config.ReportConfig;
import com.github.gsmpro.brokerage.dto.Company;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

@Service
@RequiredArgsConstructor
public class CompanyService {
    private final Map<String, String> tickersData = new HashMap<>();
    private final Map<String, String> rawQuotationSourcesData = new HashMap<>();
    private final Map<String, Company> companiesData = new HashMap<>();
    private final ReportConfig reportConfig;

    @PostConstruct
    private void fillMaps() {
        reportConfig.getTickers().forEach(ticker -> {
            var parts = ticker.split("\\$");
            if (parts.length == 2) {
                tickersData.put(parts[0], parts[1]);
            }
        });
        reportConfig.getRawQuotationSources().forEach(quotationSource -> {
            var parts = quotationSource.split("\\$");
            if (parts.length == 2) {
                rawQuotationSourcesData.put(parts[0], parts[1]);
            }
        });
    }

    public void startCurrency() {
        companiesData.clear();
    }

    public void startCompany(String name) {
        checkCompany(name);
        companiesData.get(name).startCompany();
    }

    public void endCompany(String name) {
        checkCompany(name);
        companiesData.get(name).endCompany();
    }

    public void transact(LocalDateTime time, String name, double value, boolean isBuyDeal) {
        if (isBuyDeal) {
            companiesData.get(name).buyOne(time, value);
        } else {
            companiesData.get(name).sellOne(time, value);
        }
    }

    public List<Company> getSortedRoster() {
        var companiesRoster = companiesData.values().stream()
                .filter(Company::isActive)
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName())) // sort order for active
                .collect(Collectors.toList());
        companiesRoster.addAll(companiesData.values().stream()
                .filter(not(Company::isActive))
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName())) // sort order for inactive
                .collect(Collectors.toList()));
        return companiesRoster;
    }

    public String getRawQuotationLink(String ticker, String currency) {
        if (!rawQuotationSourcesData.containsKey(currency)) {
            throw new UnsupportedOperationException(String.format("Unsupported currency %s", currency));
        }
        return rawQuotationSourcesData.get(currency) + ticker;
    }

    private void checkCompany(String name) {
        if (!tickersData.containsKey(name)) {
            throw new UnsupportedOperationException(String.format("Unsupported company %s", name));
        }
        if (!companiesData.containsKey(name)) {
            companiesData.put(name, new Company(name, tickersData.get(name)));
        }
    }
}