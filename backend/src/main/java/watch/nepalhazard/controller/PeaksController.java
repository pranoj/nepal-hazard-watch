package watch.nepalhazard.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import watch.nepalhazard.config.NepalPeaks;
import watch.nepalhazard.dto.Peak;

@RestController
@RequestMapping("/peaks")
public class PeaksController {

    @GetMapping
    public ResponseEntity<List<Peak>> getPeaks() {
        return ResponseEntity.ok(NepalPeaks.ALL);
    }
}
