package watch.nepalhazard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import watch.nepalhazard.entity.DataSource;

public interface DataSourceRepository extends JpaRepository<DataSource, Long> {
}
