package fr.irtx.lead.jsprit.data.instance;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VehicleTypeData {
	@JsonProperty("id")
	public String id;

	@JsonProperty("capacity")
	public int capacity;

	@JsonProperty("cost_per_day_EUR")
	public double costPerDay_EUR = 0.0;

	@JsonProperty("cost_per_km_EUR")
	public double costPerKm_EUR = 0.0;

	@JsonProperty("co2_per_km_g")
	public double co2PerKm_g = 0.0;

	@JsonProperty("energy_per_km_Wh")
	public double energy_per_km_Wh = 0.0;

	@JsonProperty("speed_km_h")
	public double speed_Km_h = 30.0;

	@JsonProperty("euclidean_distance_factor")
	public double euclideanDistanceFactor = 1.3;

	@JsonProperty("pickup_duration")
	public double pickupDuration = 60.0;

	@JsonProperty("delivery_duration")
	public double deliveryDuration = 120.0;
}
