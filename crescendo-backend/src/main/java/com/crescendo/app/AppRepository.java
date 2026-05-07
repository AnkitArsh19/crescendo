package com.crescendo.app;

import com.crescendo.shared.domain.valueobject.AppKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppRepository extends JpaRepository<App, AppKey> {
}
