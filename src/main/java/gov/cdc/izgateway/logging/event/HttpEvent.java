package gov.cdc.izgateway.logging.event;

import org.springframework.http.HttpHeaders;

import jakarta.annotation.Nullable;
import lombok.Data;

@Data
public abstract class HttpEvent {
    @Nullable
    protected Long contentLen;
    @Nullable
    protected String contentType;
    protected HttpHeaders headers = new HttpHeaders();
}
