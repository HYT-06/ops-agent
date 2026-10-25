package com.ticket.ops.workflow;

import com.ticket.ops.common.model.AnalysisRequest;
import com.ticket.ops.exception.IncidentNotFoundException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * P0 内存事件存储。后续替换为 MySQL 事故表。
 */
public class IncidentService {

    private final Map<String, IncidentRecord> incidents = new ConcurrentHashMap<>();
    private final IncidentWorkflow workflow;

    public IncidentService(IncidentWorkflow workflow) {
        this.workflow = workflow;
    }

    public IncidentRecord create(AnalysisRequest request) {
        IncidentRecord record = IncidentRecord.received(request);
        incidents.put(request.requestId(), record);
        return record;
    }

    public Optional<IncidentRecord> find(String requestId) {
        return Optional.ofNullable(incidents.get(requestId));
    }

    public IncidentRecord getRequired(String requestId) {
        return find(requestId).orElseThrow(() -> new IncidentNotFoundException(requestId));
    }

    public List<IncidentRecord> list() {
        return incidents.values().stream()
                .sorted(Comparator.comparing(IncidentRecord::createdAt).reversed())
                .toList();
    }

    public IncidentRecord advance(String requestId) {
        IncidentRecord current = getRequired(requestId);
        IncidentRecord next = workflow.advance(current);
        incidents.put(requestId, next);
        return next;
    }

    public int count() {
        return incidents.size();
    }
}
