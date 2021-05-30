package generator.sensor;

import java.io.*;
import java.net.ServerSocket;
import java.text.ParseException;
import java.util.Date;

import org.insight_centre.aceis.eventmodel.EventDeclaration;
import org.insight_centre.aceis.io.rdf.RDFFileManager;
import org.insight_centre.aceis.observations.SensorObservation;
import org.insight_centre.aceis.observations.WeatherObservation;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class AarhusWeatherStream extends Sensor implements Runnable {

	public AarhusWeatherStream(String uri, String out, Integer port, Long freq, String txtFile, EventDeclaration ed, Date start, Date end, boolean useTimeStamp) throws IOException {
		super(uri, out, port, freq, txtFile, ed, start, end, useTimeStamp);
	}

	@Override
	public void run() {
		logger.info("Starting sensor stream: (port) " + port + " - (uri) " + uri);
		try {
			SendFromSocket sendFromSocket = new SendFromSocket(uri, port, new ServerSocket(port));
			while (streamData.readRecord()) {
				Date obTime = sdf.parse(streamData.get("TIMESTAMP"));
				if (validateTime(obTime)) continue;

				WeatherObservation po = (WeatherObservation) createObservation();
				Model model = getModel(po, null);

				if(useTimeStamp) sendFromSocket.send(modelToListQuad(model, obTime));
				else sendFromSocket.send(model);

				sleep(freq);
			}
		} catch (Exception e) {
			logger.error("Unexpected thread termination");
			e.printStackTrace();
		}
	}

	public void generateFile() throws IOException, ParseException {
		String l[] = uri.split("/");
		if(l.length>0) {
			String file = l[l.length - 1] + ".nt";
			logger.info("File: " + file);
			Model model = ModelFactory.createDefaultModel();

			while (streamData.readRecord()) {
				Date obTime = sdf.parse(streamData.get("TIMESTAMP"));
				if (validateTime(obTime)) continue;

				WeatherObservation po = (WeatherObservation) this.createObservation();
				model = this.getModel(po, model);
			}
			OutputStream fileOut = new FileOutputStream(new File(out+file));
			model.write(fileOut, "N-TRIPLE");
		}
	}

	protected Model getModel(SensorObservation wo, Model m) throws NumberFormatException {
		if(m == null) m = ModelFactory.createDefaultModel();
		if (ed != null)
			for (String s : ed.getPayloads()) {
				Resource observation = initObservation(wo, m, ed, s);

				Property hasValue = m.createProperty(RDFFileManager.saoPrefix + "hasValue");
                //System.out.println("====> s:"+s.toString());
				if (s.contains("Temperature"))
					observation.addLiteral(hasValue, ((WeatherObservation) wo).getTemperature());
				else if (s.contains("Humidity"))
					observation.addLiteral(hasValue, ((WeatherObservation) wo).getHumidity());
				else if (s.contains("WindSpeed"))
					observation.addLiteral(hasValue, ((WeatherObservation) wo).getWindSpeed());
			}
		return m;
	}

	protected SensorObservation createObservation() {
		try {
			Integer hum = (!Sensor.isStringBlank(streamData.get("hum"))) ? Integer.parseInt(streamData.get("hum")) : 0;
			Double tempm = (!Sensor.isStringBlank(streamData.get("tempm"))) ? Double.parseDouble(streamData.get("tempm")) : 0.0;
			Double wspdm = (!Sensor.isStringBlank(streamData.get("wspdm"))) ? Double.parseDouble(streamData.get("wspdm")) : 0.0;
			Date obTime = (!Sensor.isStringBlank(streamData.get("TIMESTAMP"))) ? sdf.parse(streamData.get("TIMESTAMP")) : null;
			WeatherObservation wo = new WeatherObservation(tempm, hum, wspdm, obTime);
			logger.debug(ed.getServiceId() + ": streaming record @" + wo.getObTimeStamp());
			wo.setObId("AarhusWeatherObservation-" + (int) Math.random() * 1000);
			return wo;
		} catch (NumberFormatException | IOException | ParseException e) {
			e.printStackTrace();
		}
		return null;
	}
}
