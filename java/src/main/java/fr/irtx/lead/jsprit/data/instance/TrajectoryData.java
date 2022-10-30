package fr.irtx.lead.jsprit.data.instance;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TrajectoryData {
	@JsonProperty("lat")
	public double latitude;

	@JsonProperty("lng")
	public double longitude;

	@JsonProperty("t0")
	public double startTime;

	@JsonProperty("t1")
	public double endTime;

	@JsonProperty("type")
	public String type;

	static public TrajectoryData of(LocationData location, double startTime, double endTime, String type) {
		TrajectoryData item = new TrajectoryData();
		item.latitude = location.latitude;
		item.longitude = location.longitude;
		item.startTime = startTime;
		item.endTime = endTime;
		item.type = type;
		return item;
	}
}
