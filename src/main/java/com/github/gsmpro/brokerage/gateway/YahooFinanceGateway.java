package com.github.gsmpro.brokerage.gateway;

import com.github.gsmpro.brokerage.json.YahooFinanceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "yahoo-finance", url = "https://query1.finance.yahoo.com/v10/finance")
public interface YahooFinanceGateway {
    @GetMapping("/quoteSummary/{ticker}?modules=price")
    YahooFinanceResponse getYahooFinanceResponse(@PathVariable("ticker") String ticker);
}