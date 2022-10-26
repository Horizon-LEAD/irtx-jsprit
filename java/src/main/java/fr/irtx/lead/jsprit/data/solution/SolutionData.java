package fr.irtx.lead.jsprit.data.solution;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SolutionData extends KPIData {
	@JsonProperty("runtime_s")
	public double runtime_s;

	@JsonProperty("routes")
	public List<RouteData> routes = new LinkedList<>();

	@JsonProperty("objective")
	public double objective = 0.0;

	static public class OperatorSolutionData {
		@JsonProperty("cost_EUR")
		public double cost_EUR = 0.0;

		@JsonProperty("shipments")
		public int shipments = 0;

		@JsonProperty("cost_per_shipment_EUR")
		public double costPerShipment_EUR = 0.0;
	}

	@JsonProperty
	public Map<String, OperatorSolutionData> operators = new HashMap<>();

	static public class UCCSolutionData {
		@JsonProperty("cost_EUR")
		public double cost_EUR = 0.0;

		@JsonProperty("shipments")
		public int shipments = 0;

		@JsonProperty("cost_per_shipment_EUR")
		public double costPerShipment_EUR = 0.0;
	}

	@JsonProperty
	public UCCSolutionData ucc = new UCCSolutionData();
}
