package generator.sensor;

import java.io.*;
import java.net.ServerSocket;
import java.util.Date;

import org.insight_centre.aceis.eventmodel.EventDeclaration;
import org.insight_centre.aceis.io.rdf.RDFFileManager;
import org.insight_centre.aceis.observations.SensorObservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class LocationStream implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(LocationStream.class);
	private String txtFile;
	private EventDeclaration ed;
	private String uri;
	private String out;
	private Integer port;
	private Long freq;

	public LocationStream(String uri, String out, Integer port, Long freq, String txtFile, EventDeclaration ed) {
		this.txtFile = txtFile;
		this.ed = ed;
		this.uri = uri;
		this.out = out;
		this.port = port;
		this.freq = freq;
	}

	@Override
	public void run() {
		logger.info("Starting sensor stream: (port) " + port + " - (uri) " + uri);
		try {
			if (txtFile.contains("Location")) {
				SendFromSocket sendFromSocket = new SendFromSocket(uri, port, new ServerSocket(port));
				BufferedReader reader = new BufferedReader(new FileReader(txtFile));
				String strLine;
				while ((strLine = reader.readLine()) != null) {

					Model model = getModel(createObservation(strLine), null);
					sendFromSocket.send(model);

					Sensor.sleep(freq);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void generateFile() throws IOException {
		if (txtFile.contains("Location")) {
			String l[] = uri.split("/");
			if(l.length>0) {
				String file = l[l.length - 1] + ".nt";
				logger.info("File: " + file);
				Model model = ModelFactory.createDefaultModel();

				BufferedReader reader = new BufferedReader(new FileReader(txtFile));
				String strLine;
				while ((strLine = reader.readLine()) != null) {

					model = getModel(createObservation(strLine), model);
				}
				OutputStream fileOut = new FileOutputStream(new File(out+file));
				model.write(fileOut, "N-TRIPLE");
			}
		}
	}

	protected Model getModel(SensorObservation so, Model m) throws NumberFormatException {
		String coordinatesStr = so.getValue().toString();
		if(m == null) m = ModelFactory.createDefaultModel();
		double lat = Double.parseDouble(coordinatesStr.split(",")[0]);
		double lon = Double.parseDouble(coordinatesStr.split(",")[1]);

		Resource observation = Sensor.initObservation(so, m, ed, ed.getPayloads().get(0));

		Resource coordinates = m.createResource();
		coordinates.addLiteral(m.createProperty(RDFFileManager.ctPrefix + "hasLatitude"), lat);
		coordinates.addLiteral(m.createProperty(RDFFileManager.ctPrefix + "hasLongitude"), lon);

		observation.addProperty(
				m.createProperty(RDFFileManager.ssnPrefix + "featureOfInterest"),
				m.createResource("http://iot.ee.surrey.ac.uk/citypulse/datasets/aarhusculturalevents/culturalEvents_aarhus#context_do63jk2t8c3bjkfb119ojgkhs7"));

		observation.addProperty(m.createProperty(RDFFileManager.saoPrefix + "hasValue"), coordinates);
		return m;
	}

	protected SensorObservation createObservation(Object data) {
		String str = data.toString();
		String userStr = str.split("\\|")[0];
		String coordinatesStr = str.split("\\|")[1];
		SensorObservation so = new SensorObservation();
		so.setFoi(userStr);
		so.setValue(coordinatesStr);
		so.setObTimeStamp(new Date());
		so.setObId("UserLocationObservation-" + (int) Math.random() * 10000);
		return so;
	}

}
