package generator.sensor;

import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import org.insight_centre.aceis.eventmodel.EventDeclaration;
import org.insight_centre.aceis.io.EventRepository;
import org.insight_centre.aceis.io.rdf.RDFFileManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RunSensorsSocketsFromQuery {

    public void runSensors(Map<String, String> parameters) throws Exception {
        Properties prop = new Properties();
        FileInputStream fis = new FileInputStream(new File("citybench.properties"));
        prop.load(fis);
        fis.close();

        String datasetPath = prop.getProperty("dataset");
        String streamsPath = prop.getProperty("streams");
        String cqelsQueryPath = prop.getProperty("cqels_query");

        String query = loadQuery(cqelsQueryPath + "/" + parameters.get("query"));
        AtomicReference<Integer> port = new AtomicReference<>((parameters.containsKey("port")) ?
                Integer.parseInt(parameters.get("port")) : 5555);
        boolean useTimeStamp = parameters.containsKey("useTimeStamp") ?
                (parameters.get("useTimeStamp").equals("true") ? true : false) : true;
        Long freq = (parameters.containsKey("freq")) ? Long.parseLong(parameters.get("freq")) : null;

        if(!strIsBlank(datasetPath) && !strIsBlank(streamsPath) && !strIsBlank(cqelsQueryPath)) {
            RDFFileManager.initializeCQELSContext(datasetPath, ReasonerRegistry.getRDFSReasoner());
            EventRepository er = RDFFileManager.buildRepoFromFile(0);

            getStreamURLsFromQuery(query).forEach(uri -> {
                try {
                    startDemon(er, uri, port.get(), streamsPath,  (freq!=null) ? freq : (long) (Math.random() * 5 + 1),
                            useTimeStamp);
                    port.getAndSet(port.get() + 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public static String loadQuery(String path) {
        String q = "";
        String line;
        try {
            File inputFile = new File(path);
            FileReader in = new FileReader(inputFile);
            BufferedReader inputStream = new BufferedReader(in);
            while((line = inputStream.readLine()) != null) {
                q += line + "\n" ;
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return q;
    }

    private static List<String> getStreamFiles(String pathStreams) throws IOException {
        List<String> paths = new ArrayList<>();
        Files.walk(Paths.get(pathStreams)).forEach(path-> {
            if (Files.isRegularFile(path)) {
                paths.add(path.getFileName().toString());
            }
        });

        return paths;
    }

    public static List<String> getStreamURLsFromQuery(String query) throws Exception {
        List<String> resultSet = new ArrayList<>();
        Pattern pattern = Pattern.compile("([ ]*<[ ]*)([-./:a-zA-Z0-9#]*)([ ]*>[ ]*\\[[ a-zA-Z0-9]*\\])");
        Matcher matcher;
        String[] streamSegments = query.trim().replace("STREAM", "stream").split("stream");
        if (streamSegments.length == 1)
            throw new Exception("Error parsing query, no stream statements found for: " + query);
        else {
            for (String s : streamSegments) {
                matcher = pattern.matcher(s);
                if(matcher.find() && matcher.groupCount()>2)
                    resultSet.add(matcher.group(2).trim());
            }
        }

        List<String> results = new ArrayList<String>();
        results.addAll(resultSet);
        return results;
    }

    private static List startDemonsFromStreamNames(EventRepository er, List<String> streamNames, Integer port,
                                         String streams, Long freq, boolean useTimeStamp) throws Exception {
        Set<String> startedStreams = new HashSet<String>();
        List startedStreamObjects = new ArrayList();
        for (String sn : streamNames) {
            String uri = RDFFileManager.defaultPrefix + sn.split("\\.")[0];
            String path = streams + "/" + sn;
            if (!startedStreams.contains(uri)) {
                startedStreams.add(uri);

                startedStreamObjects.add(getRunnable(er, uri, port, path, freq, useTimeStamp));
                port++;
            }
        }

        return startedStreamObjects;
    }

    private static Runnable startDemon(EventRepository er, String uri, Integer port,
                                         String streams, Long freq, boolean useTimeStamp) throws Exception {
        String path = streams + "/" + uriToFileName(uri) + ".stream";
        return getRunnable(er, uri, port, path, freq, useTimeStamp);
    }

    public static Runnable getRunnable(EventRepository er, String uri, Integer port, String path, Long freq, boolean useTimeStamp) throws Exception {
        Runnable runnable;
        EventDeclaration ed = er.getEds().get(uri);
        if (ed == null) {
            System.out.println("ED not found for: " + uri);
            return null;
        }
        if (ed.getEventType().contains("traffic")) {
            runnable = new AarhusTrafficStream(uri, null, port, freq, path, ed, null, null, useTimeStamp);
        } else if (ed.getEventType().contains("pollution")) {
            runnable = new AarhusPollutionStream(uri, null, port, freq, path, ed, null, null, useTimeStamp);
        } else if (ed.getEventType().contains("weather")) {
            runnable = new AarhusWeatherStream(uri, null, port, freq, path, ed, null, null, useTimeStamp);
        } else if (ed.getEventType().contains("location"))
            runnable = new LocationStream(uri, null, port, freq, path, ed);
        else if (ed.getEventType().contains("parking"))
            runnable = new AarhusParkingStream(uri, null, port, freq, path, ed, null, null, useTimeStamp);
        else
            throw new Exception("Sensor type not supported: " + ed.getEventType());
        new Thread(runnable).start();

        return runnable;
    }

    public static String uriToFileName(String uri) {
        Pattern pattern = Pattern.compile("([-./:a-zA-Z0-9]*)#([A-Za-z0-9]*)");
        Matcher matcher = pattern.matcher(uri);
        if(matcher.find() && matcher.groupCount()>1) return matcher.group(2);
        return null;
    }

    public static boolean strIsBlank(String s) {
        return s == null || s.isEmpty();
    }
}
