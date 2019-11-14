package generator.sensor;

import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import org.insight_centre.aceis.eventmodel.EventDeclaration;
import org.insight_centre.aceis.io.EventRepository;
import org.insight_centre.aceis.io.rdf.RDFFileManager;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

import static generator.sensor.RunSensorsSocketsFromQuery.*;

public class GenerateFilesFromQuery {

    public void generateSensors(Map<String, String> parameters) throws Exception {
        Properties prop = new Properties();
        FileInputStream fis = new FileInputStream(new File("citybench.properties"));
        prop.load(fis);
        fis.close();

        String datasetPath = prop.getProperty("dataset");
        String streamsPath = prop.getProperty("streams");
        String cqelsQueryPath = prop.getProperty("cqels_query");

        if(!strIsBlank(datasetPath) && !strIsBlank(streamsPath) && !strIsBlank(cqelsQueryPath)) {
            String query = loadQuery(cqelsQueryPath + "/" + parameters.get("query"));
            boolean useTimeStamp = (parameters.containsKey("useTimeStamp")) ?
                    (boolean) parameters.containsKey("useTimeStamp") : false;

            RDFFileManager.initializeCQELSContext(datasetPath, ReasonerRegistry.getRDFSReasoner());
            EventRepository er = RDFFileManager.buildRepoFromFile(0);

            getStreamURLsFromQuery(query).forEach(uri -> {
                String path = streamsPath + "/" + uriToFileName(uri) + ".stream";
                try {
                    generateFile(er, uri, parameters.get("out"), path, useTimeStamp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public static void generateFile(EventRepository er, String uri, String out, String path, boolean useTimeStamp) throws Exception {
        EventDeclaration ed = er.getEds().get(uri);
        if (ed == null) {
            System.out.println("ED not found for: " + uri);
            return;
        }
        if (ed.getEventType().contains("traffic"))
            (new AarhusTrafficStream(uri, out, null, null, path, ed, null, null, useTimeStamp)).generateFile();
        else if (ed.getEventType().contains("pollution"))
            (new AarhusPollutionStream(uri, out, null, null, path, ed, null, null, useTimeStamp)).generateFile();
        else if (ed.getEventType().contains("weather"))
            (new AarhusWeatherStream(uri, out, null, null, path, ed, null, null, useTimeStamp)).generateFile();
        else if (ed.getEventType().contains("location"))
            (new LocationStream(uri, out, null, null, path, ed)).generateFile();
        else if (ed.getEventType().contains("parking"))
            (new AarhusParkingStream(uri, out, null, null, path, ed, null, null, useTimeStamp)).generateFile();
        else
            throw new Exception("Sensor type not supported: " + ed.getEventType());
    }
}
