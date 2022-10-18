package fr.irtx.lead.jsprit.infrastructure;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.driver.Driver;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.util.EuclideanDistanceCalculator;

public class VehicleTypeDependentCosts implements VehicleRoutingTransportCosts {
	private final Map<String, VehicleRoutingTransportCosts> costs = new HashMap<>();

	public void addVehicleDependentCosts(String vehicleTypeId, VehicleRoutingTransportCosts costs) {
		this.costs.put(vehicleTypeId, costs);
	}

	private void checkVehicle(Vehicle vehicle) {
		if (vehicle == null) {
			throw new IllegalStateException();
		}
	}

	@Override
	public double getTransportTime(Location from, Location to, double departureTime, Driver driver, Vehicle vehicle) {
		checkVehicle(vehicle);

		return Objects.requireNonNull(costs.get(vehicle.getType().getTypeId())).getTransportTime(from, to,
				departureTime, driver, vehicle);
	}

	@Override
	public double getBackwardTransportTime(Location from, Location to, double arrivalTime, Driver driver,
			Vehicle vehicle) {
		checkVehicle(vehicle);

		return Objects.requireNonNull(costs.get(vehicle.getType().getTypeId())).getBackwardTransportTime(from, to,
				arrivalTime, driver, vehicle);
	}

	@Override
	public double getTransportCost(Location from, Location to, double departureTime, Driver driver, Vehicle vehicle) {
		if (vehicle == null) {
			return EuclideanDistanceCalculator.calculateDistance(from.getCoordinate(), to.getCoordinate());
		}

		return Objects.requireNonNull(costs.get(vehicle.getType().getTypeId())).getTransportCost(from, to,
				departureTime, driver, vehicle);
	}

	@Override
	public double getBackwardTransportCost(Location from, Location to, double arrivalTime, Driver driver,
			Vehicle vehicle) {
		checkVehicle(vehicle);

		return Objects.requireNonNull(costs.get(vehicle.getType().getTypeId())).getBackwardTransportCost(from, to,
				arrivalTime, driver, vehicle);
	}

	@Override
	public double getDistance(Location from, Location to, double departureTime, Vehicle vehicle) {
		checkVehicle(vehicle);

		return Objects.requireNonNull(costs.get(vehicle.getType().getTypeId())).getDistance(from, to, departureTime,
				vehicle);
	}

}
