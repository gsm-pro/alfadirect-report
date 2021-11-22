package com.github.gsmpro.brokerage.service;

import com.github.gsmpro.brokerage.config.ReportConfig;
import com.github.gsmpro.brokerage.dto.Company;
import com.github.gsmpro.brokerage.dto.Entry;
import com.github.gsmpro.brokerage.dto.Generated;
import com.github.gsmpro.brokerage.dto.Quotation;
import com.github.gsmpro.brokerage.utils.Utils;
import com.github.gsmpro.brokerage.xml.Root;
import jakarta.xml.bind.JAXBContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ReportService {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private static final String NO_VALUE = "---";
    private static final int QUOTATION_THREADS = 5;
    private final CompanyService companyService;
    private final QuotationService quotationService;
    private final ReportConfig reportConfig;
    private boolean isNowCoveredByPeriod;

    public Generated generate(LocalDateTime from, LocalDateTime to) throws Exception {
        var now = LocalDateTime.now();
        isNowCoveredByPeriod = now.isAfter(from) && now.isBefore(to);
        Root root;
        try {
            var content = Utils.readFileToString(reportConfig.getFilename())
                    .replaceFirst("<Report.*?>", "<Report>");
            root = (Root) JAXBContext.newInstance(Root.class)
                    .createUnmarshaller()
                    .unmarshal(new StringReader(content.substring(Boolean.compare(content.charAt(0) == '\uFEFF', false)))); // BOM handle
        } catch (Exception e) {
            root = new Root();
        }
        var entries = new ArrayList<Entry>();
        root.getTrades().getReport().getTable2().getDetails().getList().stream() // finished deals
                .filter(r -> reportConfig.getAllowedPlaces().contains(r.getPlace()))
                .forEach(r -> entries.add(new Entry(r)));
        root.getTrades().getReport().getTable3().getDetails().getList().stream() // unfinished deals
                .filter(r -> reportConfig.getAllowedPlaces().contains(r.getPlace()))
                .forEach(r -> entries.add(new Entry(r)));
        var rows = new ArrayList<List<String>>();
        var firstDealDescription = new AtomicReference<>(NO_VALUE);
        var lastDealDescription = new AtomicReference<>(NO_VALUE);
        var firstDealOverall = new AtomicReference<>(LocalDateTime.MAX);
        var lastDealOverall = new AtomicReference<>(LocalDateTime.MIN);
        for (var groupedByCurrency : entries.stream()
                .filter(e -> e.getTime().isAfter(from) && e.getTime().isBefore(to))
                .collect(Collectors.groupingBy(Entry::getCurrency)).entrySet()) {
            companyService.startCurrency();
            for (var groupedByCompany : groupedByCurrency.getValue().stream()
                    .collect(Collectors.groupingBy(Entry::getCompany)).entrySet()) {
                var companyName = groupedByCompany.getKey();
                companyService.startCompany(companyName);
                groupedByCompany.getValue()
                        .forEach(e -> IntStream.range(0, e.getQty())
                                .forEach(q ->
                                        companyService.transact(e.getTime(), companyName, e.getPrice(), e.isBuyDeal())));
                companyService.endCompany(companyName);
            }
            var totalClosedBuy = new AtomicReference<>(.0);
            var totalClosedSell = new AtomicReference<>(.0);
            var currentBuy = new AtomicReference<>(.0);
            var equity = new AtomicReference<>(.0);
            var roster = companyService.getSortedRoster();
            var executorService = Executors.newFixedThreadPool(QUOTATION_THREADS);
            var company2quotation = new HashMap<String, Future<Quotation>>();
            var row = new ArrayList<String>();
            row.add(Utils.readFileToString(reportConfig.getHeaderLayoutFile()));
            roster.stream()
                    .filter(Company::isActive)
                    .forEach(company -> company2quotation.put(company.getTicker(),
                            executorService.submit(() ->
                                    isNowCoveredByPeriod
                                            ? quotationService.getQuotation(company.getTicker(), groupedByCurrency.getKey())
                                            : Quotation.DEFAULT)));
            executorService.shutdown();
            roster.forEach((ThrowingConsumer<Company>) company -> {
                var minDealDescription = DATE_TIME_FORMATTER.format(company.getMinDate()) + " " + company.getFirstOperation();
                var maxDealDescription = DATE_TIME_FORMATTER.format(company.getMaxDate()) + " " + company.getLastOperation();
                totalClosedBuy.updateAndGet(v -> v + company.getTotalClosedBuy());
                totalClosedSell.updateAndGet(v -> v + company.getTotalClosedSell());
                currentBuy.updateAndGet(v -> v - company.getTotalBalance());
                if (company.isActive()) {
                    var quotation = company2quotation.get(company.getTicker()).get();
                    var price = Math.abs(company.getTotalBalance() / company.getCurrentQty());
                    equity.updateAndGet(v -> v + (quotation.value() - price) * company.getCurrentQty());
                    row.add(String.format(
                            Utils.readFileToString(reportConfig.getColumnsOpenedLayoutFile()),
                            filterValueByCoverage(getProfitOrLossStyleName((quotation.value() - price) * company.getCurrentQty())),
                            company.getName(),
                            company.getTicker(),
                            getProfitOrLossStyleName(company.getTotalClosedBalance()),
                            getValueAndChangePercentageSafely(company.getTotalClosedBalance(), company.getTotalClosedBuy()),
                            company.getTotalClosedBuy(),
                            company.getTotalClosedSell(),
                            minDealDescription,
                            maxDealDescription,
                            filterValueByNullability(company.getLastBuyPrice()),
                            filterValueByNullability(company.getLastSellPrice()),
                            company.getCurrentQty(),
                            String.format("%.2f", price),
                            0 - company.getTotalBalance(), // get rid of -0,00
                            filterValueByCoverage(String.format("<a class=\"quotation-link\" href=\"%s\" rel=\"noopener noreferrer\" target=\"_blank\">%s</a>",
                                    quotation.link(),
                                    String.format("%.2f", quotation.value()))),
                            filterValueByCoverage(getProfitOrLossStyleName((quotation.value() - price) * company.getCurrentQty())),
                            filterValueByCoverage(String.format("%.2f<br>%.2f%%",
                                    (quotation.value() - price) * company.getCurrentQty(),
                                    (quotation.value() - price) * 100 * Math.signum(company.getCurrentQty()) / price)),
                            company.isWasShorted()
                                    ? "Y"
                                    : "N"));
                } else {
                    row.add(String.format(
                            Utils.readFileToString(reportConfig.getColumnsClosedLayoutFile()),
                            company.getName(),
                            company.getTicker(),
                            getProfitOrLossStyleName(company.getTotalClosedBalance()),
                            getValueAndChangePercentageSafely(company.getTotalClosedBalance(), company.getTotalClosedBuy()),
                            company.getTotalClosedBuy(),
                            company.getTotalClosedSell(),
                            minDealDescription,
                            maxDealDescription,
                            filterValueByNullability(company.getLastBuyPrice()),
                            filterValueByNullability(company.getLastSellPrice()),
                            company.isWasShorted()
                                    ? "Y"
                                    : "N"));
                }
                if (firstDealOverall.get().isAfter(company.getMinDate())) {
                    firstDealOverall.set(company.getMinDate());
                    firstDealDescription.set(minDealDescription);
                }
                if (lastDealOverall.get().isBefore(company.getMaxDate())) {
                    lastDealOverall.set(company.getMaxDate());
                    lastDealDescription.set(maxDealDescription);
                }
            });
            row.add(String.format(
                    Utils.readFileToString(reportConfig.getSummaryLayoutFile()),
                    groupedByCurrency.getKey(),
                    getProfitOrLossStyleName(totalClosedSell.get() - totalClosedBuy.get()),
                    getValueAndChangePercentageSafely(totalClosedSell.get() - totalClosedBuy.get(), totalClosedBuy.get()),
                    totalClosedBuy.get(),
                    totalClosedSell.get(),
                    currentBuy.get(),
                    filterValueByCoverage(getProfitOrLossStyleName(equity.get())),
                    filterValueByCoverage(getValueAndChangePercentageSafely(equity.get(), currentBuy.get()))));
            rows.add(row);
        }
        return new Generated(rows,
                firstDealDescription.get(),
                lastDealDescription.get(),
                DATE_TIME_FORMATTER.format(now));
    }

    private String filterValueByCoverage(String value) {
        return isNowCoveredByPeriod
                ? value
                : "";
    }

    private String filterValueByNullability(Double value) {
        return value == null
                ? ""
                : String.format("%.2f", value);
    }

    private String getProfitOrLossStyleName(double value) {
        return value > 0
                ? "profit"
                : value < 0
                ? "loss"
                : "";
    }

    private String getValueAndChangePercentageSafely(double dx, double x) {
        return String.format("%.2f<br>%s",
                dx,
                x == 0
                        ? NO_VALUE
                        : String.format("%.2f%%", dx * 100 * Math.signum(x) / x));
    }

    @FunctionalInterface
    public interface ThrowingConsumer<T> extends Consumer<T> {
        @Override
        default void accept(T t) {
            try {
                acceptThrows(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        void acceptThrows(T t) throws Exception;
    }
}