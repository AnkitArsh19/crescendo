package com.crescendo.security;

import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.User_commandRepository;
import com.crescendo.user.user_command.user_credential.UserCredentialRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Implements Spring Security's UserDetailsService contract.
 * Called by Spring's DaoAuthenticationProvider during username/password login
 * to load a user by their identifier (email or username).
 * The returned UserDetails is compared against the submitted credentials.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

	private final User_commandRepository userRepository;
	private final UserCredentialRepository credentialRepository;

	public AppUserDetailsService(User_commandRepository userRepository,
                                 UserCredentialRepository credentialRepository) {
		this.userRepository = userRepository;
		this.credentialRepository = credentialRepository;
	}

	/// readOnly = true — this method only reads from the DB, so we skip acquiring a write lock
	/// and hint to the DB that no dirty writes should be flushed after this transaction.
	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		/// Spring Security calls this with the "username" from the JWT subject — which is the email.
		User_command user = userRepository
				.findByEmailIgnoreCase(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

		return AppUserDetails.from(user, credentialRepository.findByUser_Id(user.getId()));
	}
}
