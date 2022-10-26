package fr.irtx.lead.jsprit.data.instance;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UCCData {
	@JsonProperty("location")
	public LocationData location = null;

	@JsonProperty("vehicle_types")
	public List<String> vehicleTypeIds = new LinkedList<>();

	@JsonProperty("daily_driver_salary_EUR")
	public double dailyDriverSalary_EUR = 0.0;
}
