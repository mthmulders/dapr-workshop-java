package spring;

import org.springframework.cloud.sleuth.BaggageInScope;
import org.springframework.cloud.sleuth.CurrentTraceContext;
import org.springframework.cloud.sleuth.TraceContext;
import org.springframework.cloud.sleuth.Tracer;
import reactor.util.context.Context;

import java.util.Map;
import java.util.function.Function;

public class SleuthDaprTracingInjector implements Function<Context, Context> {
    private static final String TRACE_PARENT = "traceparent";
    private static final String TRACE_STATE = "tracestate";

    @Override
    public Context apply(final Context context) {
        var tracer = context.get(Tracer.class);
        var tracingContext = context.get(CurrentTraceContext.class).context();

        final Map<String, String> map = extractTelemetryContext(tracer, tracingContext);
        return context.putAll(Context.of(map).readOnly());
    }

    private Map<String, String> extractTelemetryContext(final Tracer tracer, final TraceContext context) {
        var tracestate = calculateTraceState(tracer, context);
        var traceparent = calculateTraceParent(context);

        if (tracestate != null) {
            return Map.of(
                    TRACE_STATE, tracestate,
                    TRACE_PARENT, traceparent
            );
        } else {
            return Map.of(
                    TRACE_PARENT, traceparent
            );
        }
    }

    // Copied from Sleuth's W3CPropagation

    private static final int BYTE_BASE16 = 2;
    private static final int LONG_BYTES = Long.SIZE / Byte.SIZE;
    private static final int LONG_BASE16 = BYTE_BASE16 * LONG_BYTES;

    private static final String VERSION = "00";
    private static final char TRACEPARENT_DELIMITER = '-';

    private static final int VERSION_SIZE = 2;
    private static final int TRACEPARENT_DELIMITER_SIZE = 1;
    private static final int TRACE_ID_HEX_SIZE = 2 * LONG_BASE16;
    private static final int TRACE_ID_OFFSET = VERSION_SIZE + TRACEPARENT_DELIMITER_SIZE;
    private static final int SPAN_ID_OFFSET = TRACE_ID_OFFSET + TRACE_ID_HEX_SIZE + TRACEPARENT_DELIMITER_SIZE;
    private static final int SPAN_ID_SIZE = 8;
    private static final int SPAN_ID_HEX_SIZE = 2 * SPAN_ID_SIZE;
    private static final int TRACE_OPTION_OFFSET = SPAN_ID_OFFSET + SPAN_ID_HEX_SIZE + TRACEPARENT_DELIMITER_SIZE;
    private static final int FLAGS_SIZE = 1;
    private static final int TRACE_OPTION_HEX_SIZE = 2 * FLAGS_SIZE;
    private static final int TRACEPARENT_HEADER_SIZE = TRACE_OPTION_OFFSET + TRACE_OPTION_HEX_SIZE;

    private String calculateTraceParent(final TraceContext context) {
        final char[] chars = TemporaryBuffers.chars(TRACEPARENT_HEADER_SIZE);
        chars[0] = VERSION.charAt(0);
        chars[1] = VERSION.charAt(1);
        chars[2] = TRACEPARENT_DELIMITER;

        String traceId = padLeftZeros(context.traceId(), TRACE_ID_HEX_SIZE);
        for (int i = 0; i < traceId.length(); i++) {
            chars[TRACE_ID_OFFSET + i] = traceId.charAt(i);
        }
        chars[SPAN_ID_OFFSET - 1] = TRACEPARENT_DELIMITER;
        String spanId = context.spanId();
        for (int i = 0; i < spanId.length(); i++) {
            chars[SPAN_ID_OFFSET + i] = spanId.charAt(i);
        }
        chars[TRACE_OPTION_OFFSET - 1] = TRACEPARENT_DELIMITER;
        copyTraceFlagsHexTo(chars, TRACE_OPTION_OFFSET, context);

        return new String(chars, 0, TRACEPARENT_HEADER_SIZE);
    }

    private String calculateTraceState(final Tracer tracer, final TraceContext context) {
        final BaggageInScope baggage = tracer.getBaggage(context, TRACE_STATE);
        if (baggage == null) {
            return null;
        }
        return baggage.get(context);
    }

    private void copyTraceFlagsHexTo(char[] dest, int destOffset, final TraceContext context) {
        dest[destOffset] = '0';
        dest[destOffset + 1] = Boolean.TRUE.equals(context.sampled()) ? '1' : '0';
    }

    private String padLeftZeros(String inputString, int length) {
        if (inputString.length() >= length) {
            return inputString;
        }
        final StringBuilder sb = new StringBuilder();
        while (sb.length() < length - inputString.length()) {
            sb.append('0');
        }
        sb.append(inputString);

        return sb.toString();
    }
}
