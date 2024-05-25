package gov.cdc.izgateway.logging.event;

import gov.cdc.izgateway.logging.markers.MarkerObjectFieldName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
@MarkerObjectFieldName("httpResponse")
public class HttpResponseEvent extends HttpEvent {
    private Integer status;
    private String statusMsg;
}
