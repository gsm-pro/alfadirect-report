package com.github.gsmpro.brokerage.service;

import com.github.gsmpro.brokerage.config.ReportConfig;
import com.github.gsmpro.brokerage.dto.Quotation;
import com.github.gsmpro.brokerage.gateway.InternetBrowsingGateway;
import com.github.gsmpro.brokerage.gateway.YahooFinanceGateway;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuotationService {
    private static final String US_CURRENCY = "USD";
    private static final String RU_CURRENCY = "RUB";
    private final CompanyService companyService;
    private final InternetBrowsingGateway internetBrowsingGateway;
    private final YahooFinanceGateway yahooFinanceGateway;
    private final ReportConfig reportConfig;

    public Quotation getQuotation(String ticker, String currency) {
        return Boolean.TRUE.equals(reportConfig.getUseRawQuotationSources())
                ? getRawQuotation(ticker, currency)
                : getYahooQuotation(ticker, currency);
    }

    private Quotation getRawQuotation(String ticker, String currency) {
        var value = .0;
        var link = companyService.getRawQuotationLink(ticker, currency);
        var content = internetBrowsingGateway.getWebPage(link);
        switch (currency) {
            case US_CURRENCY:
                final var SEARCH_START = "<span class=\"QuoteStrip-lastPrice\">";
                final var SEARCH_END = "</span>";
                if (content.indexOf(SEARCH_START) >= 0) {
                    value = NumberUtils.toDouble(
                            content.substring(
                                    content.indexOf(SEARCH_START) + SEARCH_START.length(),
                                    content.indexOf(SEARCH_END, content.indexOf(SEARCH_START))));
                }
                break;
            case RU_CURRENCY:
                break;
        }
        return value == 0
                ? Quotation.DEFAULT
                : new Quotation(value, link);
    }

    private Quotation getYahooQuotation(String ticker, String currency) {
        var value = .0;
        var link = Quotation.YAHOO.link() + ticker;
        switch (currency) {
            case US_CURRENCY:
                var yahooFinanceResponse = yahooFinanceGateway.getYahooFinanceResponse(ticker);
                value = NumberUtils.toDouble(
                        yahooFinanceResponse.getQuoteSummary().getResult().iterator().next()
                                .getPrice().getRegularMarketPrice().getFmt());
                break;
            case RU_CURRENCY:
                break;
        }
        return value == 0
                ? Quotation.DEFAULT
                : new Quotation(value, link);
    }
}