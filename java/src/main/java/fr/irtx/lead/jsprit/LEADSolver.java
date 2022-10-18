package fr.irtx.lead.jsprit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit.Parameter;
import com.graphhopper.jsprit.core.algorithm.listener.IterationEndsListener;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem.FleetSize;
import com.graphhopper.jsprit.core.problem.job.Shipment;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.util.Solutions;

import fr.irtx.lead.jsprit.data.instance.OperatorData;
import fr.irtx.lead.jsprit.data.instance.ProblemData;
import fr.irtx.lead.jsprit.data.instance.ShipmentType;
import fr.irtx.lead.jsprit.data.instance.TrajectoryData;
import fr.irtx.lead.jsprit.data.instance.UCCData;
import fr.irtx.lead.jsprit.data.instance.VehicleTypeData;
import fr.irtx.lead.jsprit.data.solution.RouteData;
import fr.irtx.lead.jsprit.data.solution.RouteData.CarrierType;
import fr.irtx.lead.jsprit.data.solution.SolutionData;
import fr.irtx.lead.jsprit.infrastructure.InfrastructureManager;
import fr.irtx.lead.jsprit.vehicles.CustomVehicleRoutingActivityCosts;

public class LEADSolver {
	private final static Logger logger = Logger.getLogger(LEADSolver.class);

	static final int SIZE_INDEX = 0;

	private final InfrastructureManager infrastructure;

	private final int numberOfIterations;
	private final int numberOfThreads;
	private final int randomSeed;

	public LEADSolver(InfrastructureManager infrastructure, int numberOfIterations, int numberOfThreads,
			int randomSeed) {
		this.infrastructure = infrastructure;
		this.numberOfIterations = numberOfIterations;
		this.numberOfThreads = numberOfThreads;
		this.randomSeed = randomSeed;
	}

	public SolutionData solve(ProblemData problem) throws IOException {
		return solve(problem, (a, b, c, d) -> {
		});
	}

	public SolutionData solve(ProblemData problemData, ProgressObserver observer) throws IOException {
		Map<String, VehicleTypeData> vehicleTypeMap = new HashMap<>();
		problemData.vehicleTypes.forEach(vt -> vehicleTypeMap.put(vt.id, vt));

		List<PartialProblem> problems = new LinkedList<>();

		// Construct optimization problem per sender
		for (OperatorData operator : problemData.operators) {
			Location hubLocation = infrastructure.getLocation(senderLocationId(operator), operator.center);

			List<Location> deliveryLocations = new LinkedList<>();

			if (operator.consolidationType.equals(ShipmentType.none)
					&& operator.shipmentType.equals(ShipmentType.delivery)) {
				for (int receiverIndex = 0; receiverIndex < operator.demand.size(); receiverIndex++) {
					deliveryLocations.add(infrastructure.getLocation(receiverLocationId(operator, receiverIndex),
							operator.demand.get(receiverIndex)));
				}
			}

			if (!operator.consolidationType.equals(ShipmentType.none)
					&& operator.shipmentType.equals(ShipmentType.delivery)) {
				for (int receiverIndex = 0; receiverIndex < operator.demand.size(); receiverIndex++) {
					deliveryLocations
							.add(infrastructure.getLocation(uccLocationid(problemData.ucc), problemData.ucc.location));
				}
			}

			if (deliveryLocations.size() > 0) {
				String problemId = "delivery:" + operator.id;

				List<VehicleTypeData> vehicleTypes = operator.vehicleTypeIds.stream()
						.map(vtid -> vehicleTypeMap.get(vtid)).collect(Collectors.toList());

				problems.add(new PartialProblem(problemId,
						generateProblem(problemId, hubLocation, Collections.emptyList(), deliveryLocations,
								vehicleTypes, operator.serviceStartTime, operator.serviceEndTime, problemData),
						CarrierType.sender, operator.id));
			}
		}

		// Construct optimization problem per sender
		for (OperatorData operator : problemData.operators) {
			for (int receiverIndex = 0; receiverIndex < operator.demand.size(); receiverIndex++) {
				Location hubLocation = infrastructure.getLocation(receiverLocationId(operator, receiverIndex),
						operator.demand.get(receiverIndex));

				List<Location> pickupLocations = new LinkedList<>();

				if (operator.consolidationType.equals(ShipmentType.none)
						&& operator.shipmentType.equals(ShipmentType.pickup)) {
					pickupLocations.add(infrastructure.getLocation(senderLocationId(operator), operator.center));
				}

				if (operator.consolidationType.equals(ShipmentType.pickup)) {
					pickupLocations
							.add(infrastructure.getLocation(uccLocationid(problemData.ucc), problemData.ucc.location));
				}

				if (pickupLocations.size() > 0) {
					String problemId = "pickup:" + operator.id + "." + receiverIndex;

					List<VehicleTypeData> vehicleTypes = operator.vehicleTypeIds.stream()
							.map(vtid -> vehicleTypeMap.get(vtid)).collect(Collectors.toList());

					problems.add(new PartialProblem(problemId,
							generateProblem(problemId, hubLocation, pickupLocations, Collections.emptyList(),
									vehicleTypes, operator.serviceStartTime, operator.serviceEndTime, problemData),
							CarrierType.receiver, operator.id));
				}
			}
		}

		// Construct optimization problem per UCC
		{
			UCCData ucc = problemData.ucc;
			Location hubLocation = infrastructure.getLocation(uccLocationid(ucc), ucc.location);

			List<Location> pickupLocations = new LinkedList<>();
			List<Location> deliveryLocations = new LinkedList<>();

			for (OperatorData operator : problemData.operators) {
				for (int receiverIndex = 0; receiverIndex < operator.demand.size(); receiverIndex++) {
					if (operator.shipmentType.equals(ShipmentType.pickup)
							&& !operator.consolidationType.equals(ShipmentType.none)) {
						pickupLocations.add(infrastructure.getLocation(senderLocationId(operator), operator.center));
					}

					if (operator.consolidationType.equals(ShipmentType.delivery)) {
						deliveryLocations.add(infrastructure.getLocation(receiverLocationId(operator, receiverIndex),
								operator.demand.get(receiverIndex)));
					}
				}
			}

			if (pickupLocations.size() > 0 || deliveryLocations.size() > 0) {
				String problemId = "ucc";

				List<VehicleTypeData> vehicleTypes = problemData.ucc.vehicleTypeIds.stream()
						.map(vtid -> vehicleTypeMap.get(vtid)).collect(Collectors.toList());

				problems.add(new PartialProblem(
						problemId, generateProblem(problemId, hubLocation, pickupLocations, deliveryLocations,
								vehicleTypes, ucc.serviceStartTime, ucc.serviceEndTime, problemData),
						CarrierType.ucc, "$ucc$"));
			}
		}

		logger.info("Done generating VRPs (" + problems.size() + " in total)");

		SolutionData solutionData = new SolutionData();
		double solutionStartTime = System.nanoTime();

		int totalProblems = problems.size();
		int totalIterations = numberOfIterations * totalProblems;

		final AtomicInteger currentProblems = new AtomicInteger(0);
		final AtomicInteger currentIterations = new AtomicInteger(0);

		Collections.sort(problems, (a, b) -> {
			return -Integer.compare(a.vrp.getNuActivities(), b.vrp.getNuActivities());
		});

		for (PartialProblem problem : problems) {
			logger.info("Solving VRP " + problem.problemId);

			Jsprit.Builder builder = Jsprit.Builder.newInstance(problem.vrp) //
					.setRandom(new Random(randomSeed)) //
					.setProperty(Parameter.FIXED_COST_PARAM, "1.0") //
					.setProperty(Parameter.ITERATIONS, String.valueOf(numberOfIterations));

			final ExecutorService executorService;

			if (numberOfThreads > 1) {
				executorService = Executors.newFixedThreadPool(numberOfThreads);
				builder.setExecutorService(executorService, numberOfThreads);
			} else {
				executorService = null;
			}

			VehicleRoutingAlgorithm algorithm = builder.buildAlgorithm();

			algorithm.addListener(new IterationEndsListener() {
				@Override
				public void informIterationEnds(int i, VehicleRoutingProblem problem,
						Collection<VehicleRoutingProblemSolution> solutions) {
					currentIterations.incrementAndGet();
					observer.update(currentProblems.get(), totalProblems, currentIterations.get(), totalIterations);
				}
			});

			long startTime = System.nanoTime();
			Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
			long endTime = System.nanoTime();

			logger.info("  Finished after " + (1e-9 * (endTime - startTime)) + " seconds");

			if (executorService != null) {
				executorService.shutdown();
			}

			VehicleRoutingProblemSolution solution = Solutions.bestOf(solutions);
			for (VehicleRoute route : solution.getRoutes()) {
				VehicleTypeData vehicleTypeData = vehicleTypeMap.get(route.getVehicle().getType().getTypeId());

				RouteData routeData = new RouteData();
				solutionData.routes.add(routeData);

				routeData.carrierType = problem.carrierType;
				routeData.carrierId = problem.carrierId;
				routeData.vehicle_type = route.getVehicle().getVehicleTypeIdentifier().type;

				List<TourActivity> activities = new LinkedList<>(route.getActivities());
				activities.add(0, route.getStart());
				activities.add(route.getEnd());

				TourActivity previousActivity = null;
				Location previousLocation = null;

				for (TourActivity activity : activities) {
					if (previousActivity != null) {
						routeData.distance_km += infrastructure.getRoutingCosts().getDistance(
								previousActivity.getLocation(), activity.getLocation(), previousActivity.getEndTime(),
								route.getVehicle()) * 1e-3;
					}

					if (activity.getLocation() != previousLocation) {
						routeData.trajectory.add(TrajectoryData
								.of(infrastructure.getLocationData(activity.getLocation()), activity.getArrTime()));
					}

					previousActivity = activity;
					previousLocation = activity.getLocation();
				}

				routeData.co2_kg = routeData.distance_km * vehicleTypeData.co2PerKm_g * 1e-3;
				routeData.energy_kWh = routeData.distance_km * vehicleTypeData.energy_per_km_Wh * 1e-3;

				routeData.cost_EUR = routeData.distance_km *= vehicleTypeData.costPerKm_EUR;
				routeData.cost_EUR += vehicleTypeData.costPerDay_EUR;
			}

			synchronized (solutionData) {
				solutionData.objective += solution.getCost();
				solutionData.routes.forEach(solutionData::accumulate);
			}

			currentProblems.incrementAndGet();
			observer.update(currentProblems.get(), totalProblems, currentIterations.get(), totalIterations);
		}

		double solutionEndTime = System.nanoTime();
		solutionData.runtime_s = 1e-9 * (solutionEndTime - solutionStartTime);

		return solutionData;
	}

	private VehicleRoutingProblem generateProblem(String id, Location hubLocation, List<Location> pickupLocations,
			List<Location> deliveryLocations, List<VehicleTypeData> vehicleTypes, double serviceStartTime,
			double serviceEndTime, ProblemData problemData) {
		// Generate shipments
		List<Shipment> shipments = new ArrayList<>(pickupLocations.size() + deliveryLocations.size());

		for (int i = 0; i < pickupLocations.size(); i++) {
			shipments.add(Shipment.Builder.newInstance(id + ":p:" + i) //
					.setPickupLocation(pickupLocations.get(i)) //
					.setDeliveryLocation(hubLocation) //
					.addSizeDimension(SIZE_INDEX, 1) //
					// .setPickupTimeWindow(TimeWindow.newInstance(serviceStartTime, serviceEndTime)) // TODO
					// .setDeliveryTimeWindow(TimeWindow.newInstance(serviceStartTime, serviceEndTime)) // TODO
					.build());
		}

		for (int i = 0; i < deliveryLocations.size(); i++) {
			shipments.add(Shipment.Builder.newInstance(id + ":d:" + i) //
					.setPickupLocation(hubLocation) //
					.setDeliveryLocation(deliveryLocations.get(i)) //
					.addSizeDimension(SIZE_INDEX, 1) //
					// .setPickupTimeWindow(TimeWindow.newInstance(serviceStartTime, serviceEndTime)) // TODO
					// .setDeliveryTimeWindow(TimeWindow.newInstance(serviceStartTime, serviceEndTime)) // TODO
					.build());
		}

		// Generate vehicles
		List<VehicleImpl> vehicles = new LinkedList<>();

		for (VehicleTypeData vehicleTypeData : vehicleTypes) {
			VehicleType vehicleType = VehicleTypeImpl.Builder.newInstance(vehicleTypeData.id) //
					.addCapacityDimension(SIZE_INDEX, vehicleTypeData.capacity) //
					.setCostPerWaitingTime(0.0) //
					.setCostPerDistance(vehicleTypeData.costPerKm_EUR * 1e-3) //
					.setCostPerTransportTime(0.0) //
					.setCostPerServiceTime(0.0) //
					.setFixedCost(vehicleTypeData.costPerDay_EUR) //
					.build();

			vehicles.add(VehicleImpl.Builder.newInstance(vehicleType.getTypeId()) //
					.setStartLocation(hubLocation) //
					.setEndLocation(hubLocation) //
					.setReturnToDepot(true) //
					.setType(vehicleType) //
					.build());
		}

		logger.info("Generating VRP Problem " + id + " with " + shipments.size() + " shipments and " + vehicles.size()
				+ " vehicle types");

		return VehicleRoutingProblem.Builder.newInstance() //
				.setRoutingCost(infrastructure.getRoutingCosts()) //
				.setActivityCosts(new CustomVehicleRoutingActivityCosts(problemData)) //
				.setFleetSize(FleetSize.INFINITE) //
				.addAllJobs(shipments) //
				.addAllVehicles(vehicles) //
				.build();
	}

	static private class PartialProblem {
		VehicleRoutingProblem vrp;

		String problemId;
		CarrierType carrierType;
		String carrierId;

		PartialProblem(String problemId, VehicleRoutingProblem vrp, CarrierType carrierType, String carrierId) {
			this.vrp = vrp;
			this.problemId = problemId;
			this.carrierId = carrierId;
			this.carrierType = carrierType;
		}
	}

	private String receiverLocationId(OperatorData operator, int index) {
		return String.format("r:%s:%d", operator.id, index);
	}

	private String senderLocationId(OperatorData operator) {
		return String.format("s:%s", operator.id);
	}

	private String uccLocationid(UCCData ucc) {
		return String.format("$ucc");
	}

	static public interface ProgressObserver {
		void update(int currentProblems, int totalProblems, int currentIterations, int totalIterations);
	}
}
