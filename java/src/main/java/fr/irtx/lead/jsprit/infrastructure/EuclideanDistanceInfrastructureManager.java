package fr.irtx.lead.jsprit.infrastructure;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.util.Coordinate;
import com.graphhopper.jsprit.core.util.VehicleRoutingTransportCostsMatrix;

import fr.irtx.lead.jsprit.data.instance.LocationData;
import fr.irtx.lead.jsprit.data.instance.ProblemData;
import fr.irtx.lead.jsprit.data.instance.VehicleTypeData;

public class EuclideanDistanceInfrastructureManager implements InfrastructureManager {
	private final ProblemData problemData;

	private VehicleRoutingTransportCosts costs;
	private boolean needsUpdate = true;

	private final Map<String, Location> locations = new TreeMap<>();
	private final Map<String, LocationData> locationData = new TreeMap<>();

	private final GeotoolsTransformation wgsToEuclidean;
	private final GeodeticCalculator calculator;

	public EuclideanDistanceInfrastructureManager(ProblemData problemData, GeotoolsTransformation wgsToEuclidean)
			throws NoSuchAuthorityCodeException, FactoryException {
		this.problemData = problemData;
		this.calculator = new GeodeticCalculator(DefaultGeographicCRS.WGS84);
		this.wgsToEuclidean = wgsToEuclidean;
	}

	@Override
	public Location getLocation(String locationId, LocationData data) {
		if (locations.containsKey(locationId)) {
			return locations.get(locationId);
		} else {
			Coord euclideanCoord = wgsToEuclidean.transform(new Coord(data.longitude, data.latitude));

			Location location = Location.Builder.newInstance() //
					.setId(locationId) //
					.setCoordinate(new Coordinate(euclideanCoord.getX(), euclideanCoord.getY())) //
					.build();

			locations.put(locationId, location);
			locationData.put(locationId, data);
			needsUpdate = true;
			return location;
		}
	}

	@Override
	public LocationData getLocationData(Location location) {
		return Objects.requireNonNull(locationData.get(location.getId()));
	}

	@Override
	public VehicleRoutingTransportCosts getRoutingCosts() {
		if (needsUpdate) {
			VehicleTypeDependentCosts allCosts = new VehicleTypeDependentCosts();

			for (VehicleTypeData vehicleType : problemData.vehicleTypes) {
				VehicleRoutingTransportCostsMatrix.Builder builder = VehicleRoutingTransportCostsMatrix.Builder
						.newInstance(false);

				for (Location origin : locations.values()) {
					LocationData originData = locationData.get(origin.getId());

					for (Location destination : locations.values()) {
						LocationData destinationData = locationData.get(destination.getId());

						calculator.setStartingGeographicPoint(originData.longitude, originData.latitude);
						calculator.setDestinationGeographicPoint(destinationData.longitude, destinationData.latitude);

						double distance = calculator.getOrthodromicDistance() * vehicleType.euclideanDistanceFactor;
						double travelTime = distance / (vehicleType.speed_Km_h / 3.6);

						builder.addTransportTime(origin.getId(), destination.getId(), travelTime);
						builder.addTransportDistance(origin.getId(), destination.getId(), distance);
					}
				}

				allCosts.addVehicleDependentCosts(vehicleType.id, builder.build());
			}

			needsUpdate = false;
			costs = allCosts;
		}

		return costs;
	}
}
