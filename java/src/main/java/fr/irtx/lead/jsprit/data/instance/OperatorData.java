package fr.irtx.lead.jsprit.data.instance;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OperatorData {
	@JsonProperty("id")
	public String id;

	@JsonProperty("demand")
	public List<LocationData> demand = new LinkedList<>();

	@JsonProperty("center")
	public LocationData center;

	@JsonProperty("vehicle_types")
	public List<String> vehicleTypeIds = new LinkedList<>();

	@JsonProperty("shipment_type")
	public ShipmentType shipmentType = ShipmentType.delivery;

	@JsonProperty("consolidation_type")
	public ShipmentType consolidationType = ShipmentType.delivery;

	@JsonProperty("dailyDriverSalary_EUR")
	public double dailyDriverSalary_EUR = 0.0;
}
