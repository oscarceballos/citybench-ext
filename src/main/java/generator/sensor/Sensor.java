package generator.sensor;

import com.csvreader.CsvReader;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.LiteralImpl;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Quad;
import org.insight_centre.aceis.eventmodel.EventDeclaration;
import org.insight_centre.aceis.io.rdf.RDFFileManager;
import org.insight_centre.aceis.observations.SensorObservation;
import org.insight_centre.citybench.main.CityBench;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Sensor {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected CsvReader streamData;
    protected EventDeclaration ed;
    protected SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    protected Date startDate;
    protected Date endDate;
    protected String uri;
    protected String out;
    protected Integer port;
    protected Long freq;
    protected Boolean useTimeStamp;

    public Sensor(String uri, String out, Integer port, Long freq, String txtFile, EventDeclaration ed, Date start,
                  Date end, Boolean useTimeStamp) throws IOException {
        streamData = new CsvReader(String.valueOf(txtFile));
        streamData.setTrimWhitespace(false);
        streamData.setDelimiter(',');
        streamData.readHeaders();
        this.ed = ed;
        this.startDate = start;
        this.endDate = end;
        this.uri = uri;
        this.out = out;
        this.port = port;
        this.freq = freq;
        this.useTimeStamp = useTimeStamp;
    }

    public Sensor(String uri, String out, Integer port, Long freq, String txtFile, EventDeclaration ed, Date start,
                  Date end, Boolean useTimeStamp, String dateFormat) throws IOException {
        this(uri, out, port, freq, txtFile, ed, start, end, useTimeStamp);
        sdf = new SimpleDateFormat(dateFormat);
    }

    public boolean validateTime(Date obTime) {
        if (this.startDate != null && this.endDate != null) {
            if (obTime.before(this.startDate) || obTime.after(this.endDate)) {
                logger.debug(uri + ": Disgarded observation @" + obTime);
                return true;
            }
        }

        return false;
    }

    public static Resource initObservation(SensorObservation wo, Model m, EventDeclaration ed, String s) {
        Resource observation = m.createResource(RDFFileManager.defaultPrefix + wo.getObId() + UUID.randomUUID());
        CityBench.obMap.put(observation.toString(), wo);
        observation.addProperty(com.hp.hpl.jena.vocabulary.RDF.type, m.createResource(RDFFileManager.ssnPrefix + "Observation"));
        Resource serviceID = m.createResource(ed.getServiceId());
        observation.addProperty(m.createProperty(RDFFileManager.ssnPrefix + "observedBy"), serviceID);
        observation.addProperty(m.createProperty(RDFFileManager.ssnPrefix + "observedProperty"),
                m.createResource(s.split("\\|")[2]));
        return observation;
    }

    public static void sleep(Long freq) {
        try {
            Thread.sleep(freq);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Boolean isStringBlank(String str) {
        Boolean validator = str==null;
        validator |= str.trim().isEmpty();
        return validator;
    }

    public static List<Quad> modelToListQuad(Model model, Date timestamp) {
        List<Quad> list = new ArrayList<>();
        model.listStatements().toList().forEach(i -> {
            try {
                list.add(new Quad(
                        NodeFactory.createURI(XSDDatatype.XSDlong.getURI()+"#"+timestamp.getTime()),
                        NodeFactory.createURI(i.getSubject().getURI()),
                        NodeFactory.createURI(i.getPredicate().getURI()),
                        getNodeObject(i.getObject())));
            } catch (DatatypeConfigurationException e) {
                e.printStackTrace();
            }
        });
        return list;
    }

    private static Node getNodeObject(Object obj) throws DatatypeConfigurationException {
        if(obj instanceof LiteralImpl) {
            String nameFeld = String.format("XSD%s", ((LiteralImpl) obj).getDatatype().getJavaClass()
                    .getName().replace("java.lang.", "").toLowerCase());
            try {
                return NodeFactory.createLiteralByValue(
                        ((LiteralImpl) obj).getValue(),
                        (XSDDatatype) XSDDatatype.class.getField(nameFeld).get(null)
                );
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        String literalUri = obj.toString();
        literalUri = (literalUri.contains("^^")) ? '"'+literalUri.replace("<", "")
                .replace("^^", '"'+"^^<").concat(">") : literalUri;
        return NodeFactory.createLiteral(literalUri);
    }





}
