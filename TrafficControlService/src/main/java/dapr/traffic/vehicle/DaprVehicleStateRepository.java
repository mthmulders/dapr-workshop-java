package dapr.traffic.vehicle;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.State;
import spring.SleuthDaprTracingInjector;

import java.util.Optional;

public class DaprVehicleStateRepository implements VehicleStateRepository {
    private static final String DAPR_STORE_NAME = "statestore";

    private DaprClient daprClient;

    public DaprVehicleStateRepository(final DaprClient daprClient) {
        this.daprClient = daprClient;
    }

    @Override
    public VehicleState saveVehicleState(VehicleState vehicleState) {
        daprClient.saveState(DAPR_STORE_NAME, vehicleState.licenseNumber(), vehicleState)
                .contextWrite(new SleuthDaprTracingInjector())
                .block();

        return vehicleState;
    }

    @Override
    public Optional<VehicleState> getVehicleState(String licenseNumber) {
        return daprClient.getState(DAPR_STORE_NAME, licenseNumber, VehicleState.class)
                .contextWrite(new SleuthDaprTracingInjector())
                .blockOptional()
                .map(State::getValue);
    }
}
