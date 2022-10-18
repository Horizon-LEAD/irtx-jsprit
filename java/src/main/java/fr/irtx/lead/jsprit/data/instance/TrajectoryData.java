package fr.irtx.lead.jsprit.data.instance;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TrajectoryData {
	@JsonProperty("lat")
	public double latitude;

	@JsonProperty("lng")
	public double longitude;

	@JsonProperty("t")
	public double time;

	static public TrajectoryData of(LocationData location, double time) {
		TrajectoryData item = new TrajectoryData();
		item.latitude = location.latitude;
		item.longitude = location.longitude;
		item.time = time;
		return item;
	}
}
