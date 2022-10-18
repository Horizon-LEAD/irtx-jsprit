package fr.irtx.lead.jsprit.data.solution;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OptimizationStatusData {
	public enum StatusType {
		idle, running, finished
	}

	@JsonProperty("status")
	public StatusType status = StatusType.idle;

	@JsonProperty("progress")
	public double progess = Double.NaN;
}