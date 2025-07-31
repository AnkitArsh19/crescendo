package com.crescendo.user.user_query;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface User_queryRepository extends JpaRepository<User_query, UUID> {
}
