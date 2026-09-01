package watch.nepalhazard.dto;

import watch.nepalhazard.entity.Region;

public record RegionDTO(Long id, String code, String name) {

    public static RegionDTO from(Region region) {
        return new RegionDTO(region.getId(), region.getCode(), region.getName());
    }
}
