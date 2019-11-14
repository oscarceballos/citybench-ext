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
import org.insight_centre.aceis.observations.AarhusParkingObservation;
import org.insight_centre.aceis.observations.SensorObservation;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class AarhusParkingStream extends Sensor implements Runnable {

	public AarhusParkingStream(String uri, String out, Integer port, Long freq, String txtFile, EventDeclaration ed, Date start,
							   Date end, boolean useTimeStamp) throws IOException {
		super(uri, out, port, freq, txtFile, ed, start, end, useTimeStamp, "yyyy-MM-dd' 'HH:mm:ss");
	}

	@Override
	public void run() {
		logger.info("Starting sensor stream: (port) " + port + " - (uri) " + uri);
		try {
			SendFromSocket sendFromSocket = new SendFromSocket(uri, port, new ServerSocket(port));
			while (streamData.readRecord()) {
				Date updatetime = sdf.parse(streamData.get("updatetime"));
				if (validateTime(updatetime)) continue;

				AarhusParkingObservation po = (AarhusParkingObservation) this.createObservation();
				Model model = getModel(po, null);

				if(useTimeStamp) sendFromSocket.send(modelToListQuad(model, updatetime));
				else sendFromSocket.send(model);

				sleep(freq);
			}
		} catch (Exception e) {
			logger.error("Unexpected thread termination");
		}
	}

	public void generateFile() throws IOException, ParseException {
		String l[] = uri.split("/");
		if(l.length>0) {
			String file = l[l.length-1] + ".nt";
			logger.info("File: " + file);
			Model model = ModelFactory.createDefaultModel();
            while (streamData.readRecord()) {
                Date obTime = sdf.parse(streamData.get("updatetime"));
				if (validateTime(obTime)) continue;

                AarhusParkingObservation po = (AarhusParkingObservation) this.createObservation();
                model = this.getModel(po, model);
            }

            OutputStream fileOut = new FileOutputStream(new File(out+file));
            model.write(fileOut, "N-TRIPLE");
		}
	}

	protected Model getModel(SensorObservation so, Model m) {
		if(m==null) m = ModelFactory.createDefaultModel();
		Resource observation = initObservation(so, m, ed, ed.getPayloads().get(0));

		Property hasValue = m.createProperty(RDFFileManager.saoPrefix + "hasValue");
		observation.addLiteral(hasValue, ((AarhusParkingObservation) so).getVacancies());
		return m;
	}

	protected SensorObservation createObservation() {
		try {
			int vehicleCnt = Integer.parseInt(streamData.get("vehiclecount"));
			int id = Integer.parseInt(streamData.get("_id"));
			int total_spaces = Integer.parseInt(streamData.get("totalspaces"));

			String garagecode = streamData.get("garagecode");
			Date obTime = sdf.parse(streamData.get("updatetime"));
			AarhusParkingObservation apo = new AarhusParkingObservation(
					total_spaces - vehicleCnt,
					garagecode,
					"",
					0.0,
					0.0);
			apo.setObTimeStamp(obTime);
			apo.setObId("AarhusParkingObservation-" + id);
			return apo;
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			logger.error("ed parse error: " + ed.getServiceId());
			e.printStackTrace();
		}
		return null;

	}

}
