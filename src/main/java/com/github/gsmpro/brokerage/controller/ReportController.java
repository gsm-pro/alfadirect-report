package com.github.gsmpro.brokerage.controller;

import com.github.gsmpro.brokerage.config.ReportConfig;
import com.github.gsmpro.brokerage.service.ReportService;
import com.github.gsmpro.brokerage.utils.Utils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeParseException;

import static com.github.gsmpro.brokerage.service.ReportService.DATE_TIME_FORMATTER;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
@Validated
public class ReportController {
    private final ReportService reportService;
    private final ReportConfig reportConfig;

    @PostConstruct
    private void velocityInit() {
        Velocity.init();
    }

    /**
     * Usage:
     *
     * @see <a href="http://localhost:7777/report">Report of all time</a>
     * @see <a href="http://localhost:7777/report?from=2020-01-01T00:00:00&to=2021-12-31T23:59:59">Range report</a>
     * @see <a href="http://localhost:7777/report?from=2021-01-01T00:00:00">Range report</a>
     * @see <a href="http://localhost:7777/report?to=2020-12-31T23:59:59">Range report</a>
     */
    @GetMapping("")
    ResponseEntity<?> report(@RequestParam(name = "from", required = false) @Size(min = 19, max = 19) String from,
                             @RequestParam(name = "to", required = false) @Size(min = 19, max = 19) String to) throws Exception {
        LocalDateTime periodFrom, periodTo;
        try {
            periodFrom = LocalDateTime.parse(StringUtils.defaultString(from));
        } catch (DateTimeParseException e) {
            periodFrom = LocalDate.of(2000, Month.JANUARY, 1).atStartOfDay();
        }
        try {
            periodTo = LocalDateTime.parse(StringUtils.defaultString(to));
        } catch (DateTimeParseException ignored) {
            periodTo = LocalDate.of(2100, Month.JANUARY, 1).atStartOfDay().minusSeconds(1);
        }
        return ResponseEntity.ok(getReport(periodFrom, periodTo));
    }

    /**
     * Usage:
     *
     * @see <a href="http://localhost:7777/report/year/2021">Report of specified year</a>
     */
    @GetMapping("/year/{value}")
    ResponseEntity<?> report(@PathVariable("value")
                             @NotBlank @Size(min = 4, max = 4)
                             @Digits(integer = 4, fraction = 0) String value) throws Exception {
        var year = Integer.parseInt(value);
        return ResponseEntity.ok(getReport(
                LocalDate.of(year, Month.JANUARY, 1).atStartOfDay(),
                LocalDate.of(year + 1, Month.JANUARY, 1).atStartOfDay().minusSeconds(1)));
    }

    private String getReport(LocalDateTime periodFrom, LocalDateTime periodTo) throws Exception {
        // there must be no stocks at the periodFrom date
        var startTime = System.currentTimeMillis();
        var velocityContext = new VelocityContext();
        velocityContext.put("interval", reportConfig.getInterval());
        velocityContext.put("style", Utils.readFileToString(reportConfig.getStyles()));
        velocityContext.put("body", getBody(startTime, periodFrom, periodTo));
        var stringWriter = new StringWriter();
        Velocity.getTemplate(reportConfig.getTemplate()).merge(velocityContext, stringWriter);
        return stringWriter.toString();
    }

    private String getBody(long startTime, LocalDateTime from, LocalDateTime to) throws Exception {
        var generated = reportService.generate(from, to);
        var body = new StringBuilder();
        generated.rows().forEach(list -> body
                .append("<p><table>")
                .append(String.join("", list))
                .append("</table>"));
        body.insert(0, new StringBuilder()
                .append(String.format("<h5 class=\"date\">Report Date: %s. Generated in %d ms.</h5>",
                        generated.reportDate(),
                        System.currentTimeMillis() - startTime)) // endTime obtaining should be as late as possible
                .append(String.format("<h2 class=\"main-header\">Brokerage Report from %s to %s</h2>",
                        DATE_TIME_FORMATTER.format(from),
                        DATE_TIME_FORMATTER.format(to)))
                .append(String.format("<h4 class=\"sub-header\">First Deal in Range: %s</h4>",
                        generated.firstDealDescription()))
                .append(String.format("<h4 class=\"sub-header\">&nbsp;Last Deal in Range: %s</h4>",
                        generated.lastDealDescription())));
        return body.toString();
    }
}