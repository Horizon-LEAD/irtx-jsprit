package fr.irtx.lead.jsprit.data.solution;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SolutionData extends KPIData {
	@JsonProperty("runtime_s")
	public double runtime_s;

	@JsonProperty("routes")
	public List<RouteData> routes = new LinkedList<>();

	@JsonProperty("objective")
	public double objective = 0.0;
}
