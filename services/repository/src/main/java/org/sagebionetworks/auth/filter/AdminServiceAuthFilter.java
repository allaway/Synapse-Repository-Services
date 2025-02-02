package org.sagebionetworks.auth.filter;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.auth.HttpAuthUtil;
import org.sagebionetworks.auth.UserNameAndPassword;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.util.TemporaryCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("adminServiceAuthFilter")
public class AdminServiceAuthFilter extends BasicAuthServiceFilter {

	@Autowired
	public AdminServiceAuthFilter(StackConfiguration config, Consumer consumer) {
		super(config, consumer, new StackConfigKeyAndSecretProvider(config, StackConfiguration.SERVICE_ADMIN));
	}
	
	@Override
	protected Optional<UserNameAndPassword> getCredentialsFromRequest(HttpServletRequest httpRequest) {
		try {
			return HttpAuthUtil.getBasicAuthenticationCredentials(httpRequest);
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	@TemporaryCode(author = "marco.marasca@sagebase.org", comment = "This allows backward compatibility, can be removed once all the admin clients are updated.")
	@Override
	protected boolean credentialsRequired() {
		return false;
	}
	
	// Set as administrative service so that the admin user can be injected down the filter chain (e.g. for throttling, terms of use etc filters)
	@Override
	protected boolean isAdminService() {
		return true;
	}
	
	/**
	 * Note: We attempted to remove this temporary code but it failed since all of the IT integration tests depend on the calling the 
	 * administration services with an API key. See: PLFM-6973.
	 */
	@TemporaryCode(author = "marco.marasca@sagebase.org", comment = "This allows backward compatibility, can be removed once all the admin clients are updated.")
	@Override
	protected void validateCredentialsAndDoFilterInternal(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse, FilterChain filterChain, Optional<UserNameAndPassword> credentials)
			throws IOException, ServletException {
		if (credentials.isPresent()) {
			super.validateCredentialsAndDoFilterInternal(httpRequest, httpResponse, filterChain, credentials);
			return;
		}
		// Pass through to allow the previous auth mechanism to work
		filterChain.doFilter(httpRequest, httpResponse);
	}

}
