package gov.cdc.izgateway.service;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMethod;

public interface IAccessControlRegistry {
	public final String BLACKLIST_ROLE = "blacklist";
	List<String> getAllowedRoles(RequestMethod method, String path);
}