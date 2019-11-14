package generator.sensor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class Main {
    public static void main(String[] args) throws Exception {
        HashMap<String, String> parameters = new HashMap<>();
        Logger logger = LoggerFactory.getLogger(Main.class);

        for (String s : args) {
            parameters.put(s.split("=")[0].trim(), s.split("=")[1].trim());
        }

        if (!parameters.containsKey("type"))
            logger.error("Error, not found parameter 'type'");
        else if (!parameters.containsKey("query"))
            logger.error("Error, not found parameter 'query'");
        else {
            switch (parameters.get("type")) {
                case "G":
                    if(!parameters.containsKey("out"))
                        logger.error("Error, not found parameter 'out'");
                    else (new GenerateFilesFromQuery()).generateSensors(parameters);
                    break;
                case "R":
                    (new RunSensorsSocketsFromQuery()).runSensors(parameters);
                    break;
                default:
                    logger.error("Error, type " + parameters.get("type") + " is not known");
                    break;
            }
        }
    }
}
