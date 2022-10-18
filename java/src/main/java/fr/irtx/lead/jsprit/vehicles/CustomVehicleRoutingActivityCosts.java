package fr.irtx.lead.jsprit.vehicles;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingActivityCosts;
import com.graphhopper.jsprit.core.problem.cost.WaitingTimeCosts;
import com.graphhopper.jsprit.core.problem.driver.Driver;
import com.graphhopper.jsprit.core.problem.solution.route.activity.DeliveryActivity;
import com.graphhopper.jsprit.core.problem.solution.route.activity.PickupActivity;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;

import fr.irtx.lead.jsprit.data.instance.ProblemData;
import fr.irtx.lead.jsprit.data.instance.VehicleTypeData;

public class CustomVehicleRoutingActivityCosts implements VehicleRoutingActivityCosts {
	private final VehicleRoutingActivityCosts delegate = new WaitingTimeCosts();

	private final Map<String, Double> pickupDurations = new HashMap<>();
	private final Map<String, Double> deliveryDurations = new HashMap<>();

	public CustomVehicleRoutingActivityCosts(ProblemData problemData) {
		for (VehicleTypeData vehicleType : problemData.vehicleTypes) {
			pickupDurations.put(vehicleType.id, vehicleType.pickupDuration);
			deliveryDurations.put(vehicleType.id, vehicleType.deliveryDuration);
		}
	}

	@Override
	public double getActivityCost(TourActivity tourAct, double arrivalTime, Driver driver, Vehicle vehicle) {
		return delegate.getActivityCost(tourAct, arrivalTime, driver, vehicle);
	}

	@Override
	public double getActivityDuration(TourActivity tourAct, double arrivalTime, Driver driver, Vehicle vehicle) {
		if (tourAct instanceof PickupActivity) {
			return Objects.requireNonNull(pickupDurations.get(vehicle.getType().getTypeId()));
		} else if (tourAct instanceof DeliveryActivity) {
			return Objects.requireNonNull(deliveryDurations.get(vehicle.getType().getTypeId()));
		} else {
			return delegate.getActivityDuration(tourAct, arrivalTime, driver, vehicle);
		}
	}

}
