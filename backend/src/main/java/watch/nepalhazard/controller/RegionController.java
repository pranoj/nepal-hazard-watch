package watch.nepalhazard.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import watch.nepalhazard.dto.RegionDTO;
import watch.nepalhazard.repository.RegionRepository;

@RestController
@RequestMapping("/regions")
@CrossOrigin(origins = "http://localhost:5173")
public class RegionController {

    private final RegionRepository regionRepository;

    @Autowired
    public RegionController(RegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    @GetMapping
    public ResponseEntity<List<RegionDTO>> getRegions() {
        List<RegionDTO> regions = regionRepository.findAll().stream()
                .map(RegionDTO::from)
                .toList();
        return ResponseEntity.ok(regions);
    }
}