package com.mandiritransport.ops.flighttracking.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mandiritransport.ops.flighttracking.dto.FlightTrackingDtos.FieldChangeItem;
import com.mandiritransport.ops.flighttracking.dto.FlightTrackingDtos.PollDiffComparisonDto;
import com.mandiritransport.ops.flighttracking.entity.FlightTrackingPollLog;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Compares successive FlightAware polling responses (Section 7C).
 * Identifies added fields, removed fields, changed values, and unchanged values,
 * while highlighting operationally significant changes (status, departure/arrival times,
 * delays, aircraft position, cancellation, diversion).
 */
@Service
public class FlightAwareDiffService {

    private final ObjectMapper objectMapper;

    public FlightAwareDiffService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PollDiffComparisonDto comparePollLogs(FlightTrackingPollLog olderLog, FlightTrackingPollLog newerLog) {
        Map<String, String> olderFlat = flattenJsonPayload(olderLog.getResponsePayload());
        Map<String, String> newerFlat = flattenJsonPayload(newerLog.getResponsePayload());

        List<FieldChangeItem> added = new ArrayList<>();
        List<FieldChangeItem> removed = new ArrayList<>();
        List<FieldChangeItem> changed = new ArrayList<>();
        List<FieldChangeItem> significant = new ArrayList<>();
        List<String> unchangedPaths = new ArrayList<>();

        Set<String> allPaths = new LinkedHashSet<>();
        allPaths.addAll(olderFlat.keySet());
        allPaths.addAll(newerFlat.keySet());

        for (String path : allPaths) {
            boolean inOlder = olderFlat.containsKey(path);
            boolean inNewer = newerFlat.containsKey(path);
            String oldVal = olderFlat.get(path);
            String newVal = newerFlat.get(path);
            String category = classifyOperationalCategory(path);
            boolean isSignificant = category != null;

            if (!inOlder && inNewer) {
                FieldChangeItem item = new FieldChangeItem(path, null, newVal, category, isSignificant);
                added.add(item);
                if (isSignificant) {
                    significant.add(item);
                }
            } else if (inOlder && !inNewer) {
                FieldChangeItem item = new FieldChangeItem(path, oldVal, null, category, isSignificant);
                removed.add(item);
                if (isSignificant) {
                    significant.add(item);
                }
            } else if (!Objects.equals(oldVal, newVal)) {
                FieldChangeItem item = new FieldChangeItem(path, oldVal, newVal, category, isSignificant);
                changed.add(item);
                if (isSignificant) {
                    significant.add(item);
                }
            } else {
                unchangedPaths.add(path);
            }
        }

        List<String> sampleUnchanged = unchangedPaths.size() > 25
                ? unchangedPaths.subList(0, 25)
                : unchangedPaths;

        String summary = String.format(
                "Perbandingan menemukan %d perubahan nilai (%d signifikan secara operasional), %d atribut baru, %d atribut dihapus, dan %d atribut tetap sama.",
                changed.size(), significant.size(), added.size(), removed.size(), unchangedPaths.size()
        );

        return new PollDiffComparisonDto(
                olderLog.getId(),
                olderLog.getRequestTimestamp(),
                newerLog.getId(),
                newerLog.getRequestTimestamp(),
                added,
                removed,
                changed,
                significant,
                unchangedPaths.size(),
                sampleUnchanged,
                summary
        );
    }

    public Map<String, String> flattenJsonPayload(String rawJson) {
        Map<String, String> flat = new LinkedHashMap<>();
        if (rawJson == null || rawJson.isBlank()) {
            return flat;
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(
                    rawJson, new TypeReference<Map<String, Object>>() {}
            );
            flattenRecursive("", parsed, flat);
        } catch (Exception ignored) {
        }
        return flat;
    }

    private void flattenRecursive(String prefix, Object node, Map<String, String> out) {
        if (node == null) {
            out.put(prefix, "null");
            return;
        }
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                String next = prefix.isEmpty() ? key : prefix + "." + key;
                flattenRecursive(next, entry.getValue(), out);
            }
        } else if (node instanceof List<?> list) {
            for (int i = 0; i < Math.min(list.size(), 20); i++) {
                flattenRecursive(prefix + "[" + i + "]", list.get(i), out);
            }
        } else {
            out.put(prefix, String.valueOf(node));
        }
    }

    /**
     * Maps discovered JSON paths to operationally significant categories without assuming hardcoded names.
     */
    public String classifyOperationalCategory(String path) {
        if (path == null) return null;
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith("flightstatus") || lower.endsWith(".status") || lower.endsWith(".phase") || lower.endsWith("landingstatus")) {
            return "FLIGHT_STATUS";
        }
        if ((lower.contains("departure") || lower.contains("takeoff")) && lower.contains("scheduled")) {
            return "SCHEDULED_DEPARTURE";
        }
        if ((lower.contains("departure") || lower.contains("takeoff")) && lower.contains("estimated")) {
            return "ESTIMATED_DEPARTURE";
        }
        if ((lower.contains("departure") || lower.contains("takeoff")) && lower.contains("actual")) {
            return "ACTUAL_DEPARTURE";
        }
        if ((lower.contains("departure") || lower.contains("takeoff")) && lower.contains("delay")) {
            return "DEPARTURE_DELAY";
        }
        if ((lower.contains("arrival") || lower.contains("landing")) && lower.contains("scheduled")) {
            return "SCHEDULED_ARRIVAL";
        }
        if ((lower.contains("arrival") || lower.contains("landing")) && lower.contains("estimated")) {
            return "ESTIMATED_ARRIVAL";
        }
        if ((lower.contains("arrival") || lower.contains("landing")) && lower.contains("actual")) {
            return "ACTUAL_ARRIVAL";
        }
        if ((lower.contains("arrival") || lower.contains("landing")) && lower.contains("delay")) {
            return "ARRIVAL_DELAY";
        }
        if (lower.contains("position") || lower.endsWith(".lat") || lower.endsWith(".lon")
                || lower.endsWith(".latitude") || lower.endsWith(".longitude") || lower.endsWith(".alt") || lower.endsWith(".gs")) {
            return "AIRCRAFT_POSITION";
        }
        if (lower.contains("cancel")) {
            return "CANCELLATION_STATUS";
        }
        if (lower.contains("divert")) {
            return "DIVERSION_INFORMATION";
        }
        return null;
    }
}

