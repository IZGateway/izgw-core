package gov.cdc.izgateway.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.decorate.PrettyPrintingJsonGeneratorDecorator;
import net.logstash.logback.encoder.LogstashEncoder;

public class LogstashMessageSerializer extends JsonSerializer<ILoggingEvent> {
	private static final LogstashEncoder encoder = new LogstashEncoder();
	static {
		encoder.setJsonGeneratorDecorator(new PrettyPrintingJsonGeneratorDecorator());
		encoder.start();
	}
	@Override
	public void serialize(ILoggingEvent value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		byte[] result = encoder.encode(value);
		gen.writeRawValue(new String(result, StandardCharsets.UTF_8));
	}
}