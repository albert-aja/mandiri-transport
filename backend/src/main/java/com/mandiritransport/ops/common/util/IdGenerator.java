package com.mandiritransport.ops.common.util;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public final class IdGenerator {

    public static final ZoneId WIB = ZoneId.of("Asia/Jakarta");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicInteger SEQ = new AtomicInteger(10);

    private IdGenerator() {
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }

    public static String bookingCode(OffsetDateTime time) {
        return buildCode("BKG", time);
    }

    public static String tripCode(OffsetDateTime time) {
        return buildCode("TRP", time);
    }

    public static String handoverCode(OffsetDateTime time) {
        return buildCode("CHO", time);
    }

    public static String driverCode() {
        int next = 100 + (SEQ.incrementAndGet() % 899);
        return "DRV-" + next;
    }

    private static String buildCode(String prefix, OffsetDateTime time) {
        OffsetDateTime effective = (time != null ? time : OffsetDateTime.now(WIB));
        String datePart = effective.atZoneSameInstant(WIB).format(DATE_FMT);
        int seqPart = (SEQ.incrementAndGet() % 90) + 10;
        int randPart = ThreadLocalRandom.current().nextInt(10, 99);
        return prefix + "-" + datePart + "-" + seqPart + randPart;
    }
}
