package generator.sensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.text.ParseException;
import java.util.Date;

import org.insight_centre.aceis.eventmodel.EventDeclaration;
import org.insight_centre.aceis.io.rdf.RDFFileManager;
import org.insight_centre.aceis.observations.PollutionObservation;
import org.insight_centre.aceis.observations.SensorObservation;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class AarhusPollutionStream extends Sensor implements Runnable {

	public AarhusPollutionStream(String uri, String out, Integer port, Long freq, String txtFile, EventDeclaration ed, Date start,
								 Date end, boolean useTimeStamp) throws IOException {
		super(uri, out, port, freq, txtFile, ed, start, end, useTimeStamp);
	}

	@Override
	public void run() {
		logger.info("Starting sensor stream: (port) " + port + " - (uri) " + uri);
		try {
			SendFromSocket sendFromSocket = new SendFromSocket(uri, port, new ServerSocket(port));
			while (streamData.readRecord()) {
				Date obTime = sdf.parse(streamData.get("timestamp"));

				if (validateTime(obTime)) continue;

				PollutionObservation po = (PollutionObservation) this.createObservation();
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
				Date obTime = sdf.parse(streamData.get("timestamp"));
				if (validateTime(obTime)) continue;

				PollutionObservation po = (PollutionObservation) this.createObservation();
				model = getModel(po, model);
			}
			OutputStream fileOut = new FileOutputStream(new File(out+file));
			model.write(fileOut, "N-TRIPLE");
		}
	}

	protected Model getModel(SensorObservation so, Model m) {
		if(m == null) m = ModelFactory.createDefaultModel();
		if (ed != null)
			for (String s : ed.getPayloads()) {
				Resource observation = initObservation(so, m, ed, s);

				Property hasValue = m.createProperty(RDFFileManager.saoPrefix + "hasValue");
				observation.addLiteral(hasValue, ((PollutionObservation) so).getApi());
			}
		return m;
	}

	protected SensorObservation createObservation() {
		try {
			int ozone = Integer.parseInt(streamData.get("ozone")), particullate_matter = Integer.parseInt(streamData
					.get("particullate_matter")), carbon_monoxide = Integer.parseInt(streamData.get("carbon_monoxide")), sulfure_dioxide = Integer
					.parseInt(streamData.get("sulfure_dioxide")), nitrogen_dioxide = Integer.parseInt(streamData
					.get("nitrogen_dioxide"));
			Date obTime = sdf.parse(streamData.get("timestamp"));
			PollutionObservation po = new PollutionObservation(0.0, 0.0, 0.0, ozone, particullate_matter,
					carbon_monoxide, sulfure_dioxide, nitrogen_dioxide, obTime);
			po.setObId("AarhusPollutionObservation-" + (int) Math.random() * 10000);
			return po;
		} catch (NumberFormatException | IOException | ParseException e) {
			e.printStackTrace();
		}
		return null;

	}
}
