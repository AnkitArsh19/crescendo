package com.crescendo.security;

import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.User_commandRepository;
import com.crescendo.user.user_command.user_credential.UserCredentialRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Service
public class AppUserDetailsService implements UserDetailsService {

	private final User_commandRepository userRepository;
	private final UserCredentialRepository credentialRepository;

	public AppUserDetailsService(User_commandRepository userRepository,
                                 UserCredentialRepository credentialRepository) {
		this.userRepository = userRepository;
		this.credentialRepository = credentialRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User_command user = userRepository.findByEmailOrUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

		return AppUserDetails.from(user, credentialRepository.findByUser_Id(user.getId()));
	}
}
