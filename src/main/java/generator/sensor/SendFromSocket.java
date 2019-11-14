package generator.sensor;

import com.hp.hpl.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.Quad;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SendFromSocket {
    private ServerSocket serverSocket;
    private Socket socket;
    private DataOutputStream dos;
    private static Map<Integer, String> demons = new HashMap<>();

    public SendFromSocket(String url, Integer port, ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        if(serverSocket!=null) {
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        demons.put(port, url);
    }

    public void send(Model model) {
        try {
            if(socket!=null) {
                dos = new DataOutputStream(socket.getOutputStream());
                if(dos != null) {
                    model.write(dos, "N-TRIPLE");
                    dos.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(List<Quad> list) {
        try {
            if(socket!=null) {
                dos = new DataOutputStream(socket.getOutputStream());
                if(dos != null)
                    for(Quad q : list) {
                        RDFDataMgr.writeQuads(dos, Collections.singleton(q).iterator());
                    }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
