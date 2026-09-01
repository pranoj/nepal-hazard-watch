package watch.nepalhazard.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import watch.nepalhazard.dto.DataSourceDTO;
import watch.nepalhazard.repository.DataSourceRepository;

@RestController
@RequestMapping("/api/data-sources")
@CrossOrigin(origins = "http://localhost:5173")
public class DataSourceController {

    private final DataSourceRepository dataSourceRepository;

    @Autowired
    public DataSourceController(DataSourceRepository dataSourceRepository) {
        this.dataSourceRepository = dataSourceRepository;
    }

    @GetMapping
    public ResponseEntity<List<DataSourceDTO>> getDataSources() {
        List<DataSourceDTO> dataSources = dataSourceRepository.findAll().stream()
                .map(DataSourceDTO::from)
                .toList();
        return ResponseEntity.ok(dataSources);
    }
}
