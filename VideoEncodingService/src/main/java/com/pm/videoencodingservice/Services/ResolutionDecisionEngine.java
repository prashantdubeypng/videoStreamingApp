package com.pm.videoencodingservice.Services;

import com.pm.videoencodingservice.model.EncodingProfile;
import com.pm.videoencodingservice.model.VideoMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure decision engine: given the source video metadata, returns
 * which encoding profiles to produce. Never upscales.
 */
public final class ResolutionDecisionEngine {

    private ResolutionDecisionEngine() {}

    /**
     * Returns encoding profiles that are at or below the source resolution.
     *
     * Rule: if source is 4K  → 2160p, 1080p, 720p, 480p
     *       if source is 1080p → 1080p, 720p, 480p
     *       if source is 720p  → 720p, 480p
     *       if source < 720p   → 480p only
     */
    public static List<EncodingProfile> decide(VideoMetadata metadata) {
        int sourceHeight = metadata.height();
        List<EncodingProfile> profiles = new ArrayList<>();

        if (sourceHeight >= 2160) profiles.add(EncodingProfile.P_2160);
        if (sourceHeight >= 1080) profiles.add(EncodingProfile.P_1080);
        if (sourceHeight >= 720)  profiles.add(EncodingProfile.P_720);

        // Always include 480p as the minimum quality tier
        profiles.add(EncodingProfile.P_480);

        return profiles;
    }
}
