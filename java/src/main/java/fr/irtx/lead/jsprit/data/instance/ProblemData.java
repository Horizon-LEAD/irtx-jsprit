package fr.irtx.lead.jsprit.data.instance;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProblemData {
	@JsonProperty("operators")
	public List<OperatorData> operators = new LinkedList<>();

	@JsonProperty("vehicle_types")
	public List<VehicleTypeData> vehicleTypes = new LinkedList<>();

	@JsonProperty("ucc")
	public UCCData ucc = null;
}
