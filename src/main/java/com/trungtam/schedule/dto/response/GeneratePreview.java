package com.trungtam.schedule.dto.response;

import java.util.List;

/** Ket qua sinh buoi (preview khi dryRun, hoac tong ket sau khi luu). */
public record GeneratePreview(
        int total,
        List<SessionPreviewLine> sessions,
        List<ConflictLine> conflicts
) {
}
