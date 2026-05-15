package com.voidvvv.kzcollision.editor.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourceRecoveryReport {
    private final Map<String, ResourceStatus> statuses = new HashMap<>();
    private final Map<String, List<String>> candidates = new HashMap<>();

    public void setStatus(String assetId, ResourceStatus status) {
        statuses.put(assetId, status);
    }

    public ResourceStatus getStatus(String assetId) {
        return statuses.get(assetId);
    }

    public void setCandidates(String assetId, List<String> paths) {
        candidates.put(assetId, new ArrayList<>(paths));
    }

    public List<String> getCandidates(String assetId) {
        List<String> found = candidates.get(assetId);
        return found == null ? Collections.emptyList() : Collections.unmodifiableList(found);
    }

    public int getMatchedCount() { return count(ResourceStatus.MATCHED); }
    public int getRepairedCount() { return count(ResourceStatus.REPAIRED); }
    public int getMissingCount() { return count(ResourceStatus.MISSING); }
    public int getConflictCount() { return count(ResourceStatus.CONFLICT); }

    private int count(ResourceStatus status) {
        int total = 0;
        for (ResourceStatus value : statuses.values()) {
            if (value == status) total++;
        }
        return total;
    }

    @Override
    public String toString() {
        return String.format("ResourceRecoveryReport{matched=%d, repaired=%d, missing=%d, conflict=%d}",
                getMatchedCount(), getRepairedCount(), getMissingCount(), getConflictCount());
    }
}
