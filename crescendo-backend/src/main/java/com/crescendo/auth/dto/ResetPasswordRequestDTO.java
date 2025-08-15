package com.crescendo.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Completing a password reset using a reset token emailed to the user.
 */
public record ResetPasswordRequestDTO(
	@NotBlank String resetToken,
	@NotBlank @Size(min = 6) String newPassword
) {}
