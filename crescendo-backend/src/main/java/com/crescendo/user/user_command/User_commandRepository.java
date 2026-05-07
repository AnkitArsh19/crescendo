package com.crescendo.user.user_command;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface User_commandRepository extends JpaRepository<User_command, UUID> {

	/// email is an @Embedded Email value object whose single field is "value".
	/// Spring Data cannot derive the path from the method name, so we use explicit JPQL.
	@Query("SELECT u FROM User_command u WHERE UPPER(u.email.value) = UPPER(:email)")
	Optional<User_command> findByEmailIgnoreCase(@Param("email") String email);
}
