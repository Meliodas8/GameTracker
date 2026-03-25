package dev.manel.gametracker.core.model;

public record DetectedGame(
        String name,
        String platform,
        String executableName,
        String platformId
) {}
