package com.voidvvv.kzcollision.editor.project;

import com.voidvvv.kzcollision.core.model.AssetType;
import com.voidvvv.kzcollision.core.model.Project;
import com.voidvvv.kzcollision.core.model.SourceAsset;
import com.voidvvv.kzcollision.core.model.SourceRegion;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ResourceResolver {
    /** Resolves each source asset against the asset index. MUTATES project source assets in place to update paths. */
    public ResourceRecoveryReport resolve(Project project, AssetIndex index) {
        Objects.requireNonNull(project, "project must not be null");
        Objects.requireNonNull(index, "index must not be null");
        ResourceRecoveryReport report = new ResourceRecoveryReport();
        for (SourceAsset asset : project.getSourceAssets()) {
            resolveAsset(asset, index, report);
        }
        return report;
    }

    private void resolveAsset(SourceAsset asset, AssetIndex index, ResourceRecoveryReport report) {
        String normalizedPath = asset.getInternalPath() == null ? null : AssetIndex.normalize(asset.getInternalPath());
        if (normalizedPath == null) {
            report.setStatus(asset.getId(), ResourceStatus.MISSING);
            return;
        }
        AssetRecord exact = index.findByInternalPath(normalizedPath);
        if (exact != null && isCompatible(asset, exact)) {
            applyRecord(asset, exact);
            report.setStatus(asset.getId(), ResourceStatus.MATCHED);
            return;
        }

        String fileName = new File(normalizedPath).getName();
        List<AssetRecord> candidates = compatibleCandidates(asset, index.findByFileName(fileName));
        if (candidates.size() == 1) {
            applyRecord(asset, candidates.get(0));
            report.setStatus(asset.getId(), ResourceStatus.REPAIRED);
            return;
        }
        if (candidates.size() > 1) {
            List<String> paths = new ArrayList<>();
            for (AssetRecord candidate : candidates) {
                paths.add(candidate.getInternalPath());
            }
            report.setCandidates(asset.getId(), paths);
            report.setStatus(asset.getId(), ResourceStatus.CONFLICT);
            return;
        }
        report.setStatus(asset.getId(), ResourceStatus.MISSING);
    }

    private List<AssetRecord> compatibleCandidates(SourceAsset asset, List<AssetRecord> records) {
        List<AssetRecord> out = new ArrayList<>();
        for (AssetRecord record : records) {
            if (isCompatible(asset, record)) out.add(record);
        }
        return out;
    }

    private boolean isCompatible(SourceAsset asset, AssetRecord record) {
        if (asset.getType() != record.getType()) return false;
        if (asset.getType() == AssetType.ATLAS) {
            for (SourceRegion region : asset.getRegions()) {
                if (record.findRegion(region.getName()) == null) return false;
            }
        }
        return true;
    }

    /** Updates the source asset in-place with the matched record's internal path and region bounds. */
    private void applyRecord(SourceAsset asset, AssetRecord record) {
        asset.setInternalPath(record.getInternalPath());
        if (asset.getType() == AssetType.ATLAS) {
            for (SourceRegion region : asset.getRegions()) {
                AssetRegionRecord indexedRegion = record.findRegion(region.getName());
                if (indexedRegion != null) {
                    region.setBounds(indexedRegion.getBounds());
                }
            }
        }
    }
}
