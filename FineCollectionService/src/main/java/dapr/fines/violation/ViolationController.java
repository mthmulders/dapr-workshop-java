package dapr.fines.violation;

import com.fasterxml.jackson.databind.JsonNode;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
public class ViolationController {
    private final ViolationProcessor violationProcessor;

    public ViolationController(final ViolationProcessor violationProcessor) {
        this.violationProcessor = violationProcessor;
    }

    @Topic(name = "speedingviolations", pubsubName = "pubsub")
    @PostMapping(path = "/collectfine")
    public ResponseEntity<Void> registerViolation(@RequestBody final CloudEvent<SpeedingViolation> event) {
        var violation = event.getData();
        violationProcessor.processSpeedingViolation(violation);
        return ResponseEntity.ok().build();
    }
}
