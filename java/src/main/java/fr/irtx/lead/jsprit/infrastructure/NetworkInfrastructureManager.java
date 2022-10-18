package fr.irtx.lead.jsprit.infrastructure;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.vehicles.Vehicle;

import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.util.Coordinate;
import com.graphhopper.jsprit.core.util.VehicleRoutingTransportCostsMatrix;

import fr.irtx.lead.jsprit.data.instance.LocationData;
import fr.irtx.lead.jsprit.data.instance.ProblemData;
import fr.irtx.lead.jsprit.data.instance.VehicleTypeData;

public class NetworkInfrastructureManager implements InfrastructureManager {
	private final ProblemData problemData;

	private final Network network;
	private final QuadTree<Node> spatialIndex;

	private VehicleRoutingTransportCosts costs;
	private boolean needsUpdate = true;

	private final Map<String, Location> locations = new TreeMap<>();
	private final Map<String, LocationData> locationData = new TreeMap<>();

	private GeotoolsTransformation wgsToEuclidean;
	private double freespeedFactor;

	public NetworkInfrastructureManager(ProblemData problemData, Network network, GeotoolsTransformation wgsToEuclidean,
			double freespeedFactor) {
		double dimensions[] = NetworkUtils.getBoundingBox(network.getNodes().values());

		this.problemData = problemData;
		this.network = network;
		this.wgsToEuclidean = wgsToEuclidean;
		this.freespeedFactor = freespeedFactor;

		this.spatialIndex = new QuadTree<>(dimensions[0], dimensions[1], dimensions[2], dimensions[3]);

		for (Node node : network.getNodes().values()) {
			spatialIndex.put(node.getCoord().getX(), node.getCoord().getY(), node);
		}
	}

	public VehicleRoutingTransportCosts getRoutingCosts() {
		if (needsUpdate) {
			VehicleTypeDependentCosts allCosts = new VehicleTypeDependentCosts();
			SpeedyALTFactory routerFactory = new SpeedyALTFactory();

			for (VehicleTypeData vehicleTypeData : problemData.vehicleTypes) {
				LimitedTravelTime travelTime = new LimitedTravelTime(vehicleTypeData.speed_Km_h / 3.6);
				LeastCostPathCalculator router = routerFactory.createPathCalculator(network,
						new DistanceTravelDisutility(), travelTime);

				VehicleRoutingTransportCostsMatrix.Builder builder = VehicleRoutingTransportCostsMatrix.Builder
						.newInstance(false);

				for (var origin : locations.entrySet()) {
					Coord originCoordinate = wgsToEuclidean.transform(new Coord(
							origin.getValue().getCoordinate().getX(), origin.getValue().getCoordinate().getY()));
					Node originNode = spatialIndex.getClosest(originCoordinate.getX(), originCoordinate.getY());

					for (var destination : locations.entrySet()) {
						Coord destinationCoordinate = wgsToEuclidean
								.transform(new Coord(destination.getValue().getCoordinate().getX(),
										destination.getValue().getCoordinate().getY()));
						Node destinationNode = spatialIndex.getClosest(destinationCoordinate.getX(),
								destinationCoordinate.getY());

						Path path = router.calcLeastCostPath(originNode, destinationNode, 0.0, null, null);

						builder.addTransportTime(origin.getKey(), destination.getKey(), path.travelTime);
						builder.addTransportDistance(origin.getKey(), destination.getKey(),
								RouteUtils.calcDistance(path));
					}
				}

				needsUpdate = false;
				allCosts.addVehicleDependentCosts(vehicleTypeData.id, builder.build());
			}

			costs = allCosts;
		}

		return costs;
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

	private class DistanceTravelDisutility implements TravelDisutility {
		@Override
		public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
			return link.getLength();
		}

		@Override
		public double getLinkMinimumTravelDisutility(Link link) {
			return link.getLength();
		}
	}

	private class LimitedTravelTime implements TravelTime {
		private double maximumSpeed;

		LimitedTravelTime(double maximumSpeed) {
			this.maximumSpeed = maximumSpeed;
		}

		@Override
		public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
			double speed = Math.min(freespeedFactor * link.getFreespeed(), maximumSpeed);
			return link.getLength() / speed;
		}
	}
}
