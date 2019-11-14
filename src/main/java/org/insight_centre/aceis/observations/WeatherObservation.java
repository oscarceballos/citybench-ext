package org.insight_centre.aceis.observations;

import java.util.Date;

public class WeatherObservation extends SensorObservation {
	Integer humidity;
	Double windSpeed, temperature;

	public WeatherObservation(Double temperature, Integer humidity, Double windSpeed, Date timeStamp) {
		super();
		this.temperature = temperature;
		this.humidity = humidity;
		this.windSpeed = windSpeed;
		this.obTimeStamp = timeStamp;
	}

	public Double getTemperature() {
		return temperature;
	}

	public void setTemperature(Double temperature) {
		this.temperature = temperature;
	}

	public Double getWindSpeed() {
		return windSpeed;
	}

	public void setWindSpeed(Double windSpeed) {
		this.windSpeed = windSpeed;
	}

	public Integer getHumidity() {
		return humidity;
	}

	public void setHumidity(Integer humidity) {
		this.humidity = humidity;
	}
}
