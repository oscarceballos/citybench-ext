package generator.sensor;

import java.io.*;
import java.net.ServerSocket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.insight_centre.aceis.eventmodel.EventDeclaration;
import org.insight_centre.aceis.eventmodel.TrafficReportService;
import org.insight_centre.aceis.io.rdf.RDFFileManager;
import org.insight_centre.aceis.observations.AarhusTrafficObservation;
import org.insight_centre.aceis.observations.SensorObservation;

import com.csvreader.CsvReader;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class AarhusTrafficStream extends Sensor implements Runnable {
	private String distance;
	private CsvReader metaData;

	public AarhusTrafficStream(String uri, String out, Integer port, Long freq, String txtFile, EventDeclaration ed, Date start,
							   Date end, boolean useTimeStamp) throws IOException {
		super(uri, out, port, freq, txtFile, ed, start, end, useTimeStamp);

		metaData = new CsvReader("dataset/MetaData/trafficMetaData.csv");
		metaData.readHeaders();

		while (metaData.readRecord()) {
			if (streamData.get("REPORT_ID").equals(metaData.get("REPORT_ID"))) {
				distance = metaData.get("DISTANCE_IN_METERS");
				metaData.close();
				break;
			}
		}
	}

	protected SensorObservation createObservation() {
		try {
			AarhusTrafficObservation data;
			data = new AarhusTrafficObservation(Double.parseDouble(streamData.get("REPORT_ID")),
					Double.parseDouble(streamData.get("avgSpeed")), Double.parseDouble(streamData.get("vehicleCount")),
					Double.parseDouble(streamData.get("avgMeasuredTime")), 0, 0, null, null, 0.0, 0.0, null, null, 0.0,
					0.0, null, null, streamData.get("TIMESTAMP"));
			String obId = "AarhusTrafficObservation-" + streamData.get("_id");
			Double distance = Double.parseDouble(((TrafficReportService) ed).getDistance() + "");
			if (data.getAverageSpeed() != 0)
				data.setEstimatedTime(distance / data.getAverageSpeed());
			else
				data.setEstimatedTime(-1.0);
			if (distance != 0)
				data.setCongestionLevel(data.getVehicle_count() / distance);
			else
				data.setCongestionLevel(-1.0);
			data.setObId(obId);
			return data;
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	protected Model getModel(SensorObservation data, Model m) throws NumberFormatException {
		if(m == null) m = ModelFactory.createDefaultModel();
		if (ed != null)
			for (String pStr : ed.getPayloads()) {
				String obId = data.getObId();
				Resource observation = initObservation(data, m, ed, pStr);
						m.createResource(RDFFileManager.defaultPrefix + obId + UUID.randomUUID());

				Property hasValue = m.createProperty(RDFFileManager.saoPrefix + "hasValue");
				if (pStr.contains("AvgSpeed"))
					observation.addLiteral(hasValue, ((AarhusTrafficObservation) data).getAverageSpeed());
				else if (pStr.contains("VehicleCount")) {
					double value = ((AarhusTrafficObservation) data).getVehicle_count();
					observation.addLiteral(hasValue, value);
				} else if (pStr.contains("MeasuredTime"))
					observation.addLiteral(hasValue, ((AarhusTrafficObservation) data).getAvgMeasuredTime());
				else if (pStr.contains("EstimatedTime"))
					observation.addLiteral(hasValue, ((AarhusTrafficObservation) data).getEstimatedTime());
				else if (pStr.contains("CongestionLevel"))
					observation.addLiteral(hasValue, ((AarhusTrafficObservation) data).getCongestionLevel());
			}
		return m;
	}

	@Override
	public void run() {
		logger.info("Starting sensor stream: (port) " + port + " - (uri) " + uri);
		try {
			SendFromSocket sendFromSocket = new SendFromSocket(uri, port, new ServerSocket(port));

			while (streamData.readRecord()) {
				Date obTime = sdf.parse(streamData.get("TIMESTAMP"));
				if (validateTime(obTime)) continue;

				AarhusTrafficObservation data = (AarhusTrafficObservation) this.createObservation();
				Model model = getModel(data, null);

				if(useTimeStamp) sendFromSocket.send(modelToListQuad(model, obTime));
				else sendFromSocket.send(model);

				sleep(freq);
			}
		} catch (Exception e) {
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
				Date obTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(streamData.get("TIMESTAMP"));

				if (validateTime(obTime)) continue;

				AarhusTrafficObservation data = (AarhusTrafficObservation) this.createObservation();
				model = this.getModel(data, model);
			}
			OutputStream fileOut = new FileOutputStream(new File(out+file));
			model.write(fileOut, "N-TRIPLE");
		}
	}
}