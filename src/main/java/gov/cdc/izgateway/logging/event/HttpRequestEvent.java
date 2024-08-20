package gov.cdc.izgateway.logging.event;

import org.springframework.http.HttpMethod;

import gov.cdc.izgateway.logging.markers.MarkerObjectFieldName;
import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
@MarkerObjectFieldName("httpRequest")
public class HttpRequestEvent extends HttpEvent  {
	@Nullable
    private String authType;
	@Nullable
    private String contextPath;
	@Nullable
    private String localName;
	@Nullable
    private Integer localPort;
    private HttpMethod method;
    private String pathInfo;
    private String protocol;
    private String queryString;
    @Nullable
    private String remoteAddr;
    private String remoteHost;
    private Integer remotePort;
    private String scheme;
    @Nullable
    private String serverName;
    @Nullable
    private Integer serverPort;
    @Nullable
    private String servletPath;
    private String uri;
    private String url;
    @Nullable
    private String userPrincipal;
}
