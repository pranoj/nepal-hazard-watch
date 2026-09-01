package watch.nepalhazard.dto;

import watch.nepalhazard.entity.Region;

public record RegionDTO(Long id, String name, Double latitude, Double longitude, String riskLevel, Integer population) {

    public static RegionDTO from(Region region) {
        return new RegionDTO(
                region.getId(),
                region.getName(),
                region.getLatitude(),
                region.getLongitude(),
                region.getRiskLevel(),
                region.getPopulation());
    }
}