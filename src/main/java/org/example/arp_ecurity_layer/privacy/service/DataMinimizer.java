package org.example.arp_ecurity_layer.privacy.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Coarsens exact transaction data into privacy-safe approximations
 * to prevent linkage attacks when forwarding to AI service.
 */
@Component
public class DataMinimizer {

    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * Converts an exact timestamp to a coarse time-of-day bucket.
     * Removes day/date/time precision to prevent linkage attacks.
     *
     * @param ts exact transaction timestamp
     * @return e.g. "WEEKDAY_MORNING", "WEEKEND_NIGHT"
     */
    public String toTimeBucket(LocalDateTime ts) {
        // TODO: Determine WEEKDAY vs WEEKEND using ts.getDayOfWeek()
        //       (SATURDAY/SUNDAY = WEEKEND, rest = WEEKDAY)
        // TODO: Classify hour into time slot:
        //       MORNING   = 06:00–11:59
        //       AFTERNOON = 12:00–16:59
        //       EVENING   = 17:00–20:59
        //       NIGHT     = 21:00–05:59 (wrap around midnight)
        // TODO: Return e.g. "WEEKDAY_EVENING", "WEEKEND_MORNING"
        if (ts == null) ts = LocalDateTime.now();
        DayOfWeek dow = ts.getDayOfWeek();
        boolean isWeekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);
        String dayType = isWeekend ? "WEEKEND" : "WEEKDAY";
        int hour = ts.getHour();

        String timeOfDay;
        if (hour >= 6 && hour < 12) timeOfDay = "MORNING";
        else if (hour >= 12 && hour < 16) timeOfDay = "AFTERNOON";
        else if (hour >= 16 && hour < 20) timeOfDay = "EVENING";
        else timeOfDay = "NIGHT";

        return dayType + "_" + timeOfDay;
    }

    /**
     * Converts exact rupee amount to a named band to prevent amount-based linkage.
     *
     * @param amount exact transaction amount
     * @return one of: MICRO_0_100 | LOW_100_500 | MEDIUM_500_2500 | HIGH_2500_10000 | VERY_HIGH_10000_PLUS
     */
    public String toAmountBand(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return "ZERO";
        else if (amount.compareTo(BigDecimal.valueOf(100)) < 0) return "MICRO_0_100";
        else if (amount.compareTo(BigDecimal.valueOf(500)) < 0) return "LOW_100_500";
        else if (amount.compareTo(BigDecimal.valueOf(2500)) < 0) return "MEDIUM_500_2500";
        else if (amount.compareTo(BigDecimal.valueOf(10000)) < 0) return "HIGH_2500_10000";
        else return "VERY_HIGH_10000_PLUS";
    }

    /**
     * Returns a minimized/rounded amount based on privacy mode.
     * BALANCED: round to nearest 500 to reduce precision.
     * STRICT: return null — only amountBand is emitted.
     *
     * @param amount      exact transaction amount
     * @param privacyMode BALANCED | STRICT
     * @return rounded amount or null
     */
    public BigDecimal toMinimizedAmount(BigDecimal amount, String privacyMode) {
        // TODO: If STRICT, return null
        if("STRICT".equalsIgnoreCase(privacyMode))return null;
        // TODO: If BALANCED, round to nearest 500:
        //       e.g. 1499 -> 1500, 1250 -> 1500, 2300 -> 2500, 400 -> 500
        //       Use: amount.divide(BigDecimal.valueOf(500), 0, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(500))
        if(amount == null)return BigDecimal.ZERO;
        BigDecimal step = BigDecimal.valueOf(500);
        return amount.divide(step, 0, RoundingMode.HALF_UP).multiply(step);
    }

    /**
     * Returns the period key (YYYY-MM) for a given timestamp.
     *
     * @param ts timestamp
     * @return e.g. "2026-11"
     */
    public String toPeriodKey(LocalDateTime ts) {
        // TODO: return ts.format(PERIOD_FORMATTER)
       if(ts == null)ts = LocalDateTime.now();
       return ts.format(PERIOD_FORMATTER);
    }
}

