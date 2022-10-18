package fr.irtx.lead.jsprit.data.solution;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import fr.irtx.lead.jsprit.data.instance.TrajectoryData;

public class RouteData extends KPIData {
	@JsonProperty("vehicle_type")
	public String vehicle_type;

	@JsonProperty("carrier")
	public String carrierId;

	@JsonProperty("carrier_type")
	public CarrierType carrierType;

	public enum CarrierType {
		sender, receiver, ucc
	}

	@JsonProperty("trajectory")
	public List<TrajectoryData> trajectory = new LinkedList<>();
}
