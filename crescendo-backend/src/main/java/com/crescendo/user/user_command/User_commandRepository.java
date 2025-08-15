package com.crescendo.user.user_command;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface User_commandRepository extends JpaRepository<User_command, UUID> {

	java.util.Optional<User_command> findByEmailIdIgnoreCase(String emailId);
	java.util.Optional<User_command> findByUserNameIgnoreCase(String userName);

	@Query("select u from User_command u where lower(u.emailId) = lower(:identifier) or lower(u.userName) = lower(:identifier)")
	java.util.Optional<User_command> findByEmailOrUsername(@Param("identifier") String identifier);
}
