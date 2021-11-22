package com.github.gsmpro.brokerage.dto;

import com.github.gsmpro.brokerage.xml.Root;
import lombok.Getter;

import java.time.LocalDateTime;

import static com.github.gsmpro.brokerage.service.ReportService.DATE_TIME_FORMATTER;

@Getter
public class Entry {
    private LocalDateTime time;
    private String company;
    private double price;
    private int qty;
    private String currency;
    private boolean isBuyDeal;

    public Entry(Root.Trades.Report.Table2.RawDetails2.RawDetail2 rawDetail2) {
        entryInner(rawDetail2.getTime(), rawDetail2.getCompany(),
                rawDetail2.getPrice(), rawDetail2.getQty(), rawDetail2.getCurrency());
    }

    public Entry(Root.Trades.Report.Table3.RawDetails3.RawDetail3 rawDetail3) {
        entryInner(rawDetail3.getTime(), rawDetail3.getCompany(),
                rawDetail3.getPrice(), rawDetail3.getQty(), rawDetail3.getCurrency());
    }

    private void entryInner(String time, String company, String price, String qty, String currency) {
        var timeValue = time.substring(0, 19);
        if (timeValue.trim().length() == 18) { // add leading zero if hours < 10
            timeValue = timeValue.replace(" ", " 0");
        }
        this.time = LocalDateTime.parse(timeValue.substring(0, 19), DATE_TIME_FORMATTER);
        this.company = company;
        this.price = Double.parseDouble(price);
        this.qty = Math.abs(Integer.parseInt(qty));
        this.currency = currency;
        isBuyDeal = !qty.startsWith("-");
    }
}