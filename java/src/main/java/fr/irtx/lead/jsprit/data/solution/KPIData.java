package fr.irtx.lead.jsprit.data.solution;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KPIData {
	@JsonProperty("cost_EUR")
	public double cost_EUR = 0.0;

	@JsonProperty("energy_kWh")
	public double energy_kWh = 0.0;

	@JsonProperty("co2_kg")
	public double co2_kg = 0.0;

	@JsonProperty("distance_km")
	public double distance_km = 0.0;

	public void accumulate(KPIData other) {
		cost_EUR += other.cost_EUR;
		energy_kWh += other.energy_kWh;
		co2_kg += other.co2_kg;
		distance_km += other.distance_km;
	}
}
