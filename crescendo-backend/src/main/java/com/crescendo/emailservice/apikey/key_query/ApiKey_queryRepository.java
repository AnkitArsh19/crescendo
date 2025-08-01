package com.crescendo.emailservice.apikey.key_query;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ApiKey_queryRepository extends JpaRepository<ApiKey_query, UUID> {
}
