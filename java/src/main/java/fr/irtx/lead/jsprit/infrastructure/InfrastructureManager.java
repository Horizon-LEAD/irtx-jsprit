package fr.irtx.lead.jsprit.infrastructure;

import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;

import fr.irtx.lead.jsprit.data.instance.LocationData;

public interface InfrastructureManager {

	Location getLocation(String locationId, LocationData data);

	LocationData getLocationData(Location location);

	VehicleRoutingTransportCosts getRoutingCosts();

}