package sample;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sun.org.apache.xpath.internal.SourceTree;
import javafx.application.Platform;
import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.transport.TransportMappings;
import org.snmp4j.util.*;

public class SimpleSnmpClient {

    private String address;

    private Snmp snmp;
    public int rows;


    public SimpleSnmpClient(String address) {
        super();
        this.address = address;
        try {
            start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() throws IOException {
        snmp.close();
    }

    private void start() throws IOException {
        UdpAddress udpAddress = new UdpAddress("127.0.0.1/162");
        TransportMapping transport = new DefaultUdpTransportMapping(udpAddress);
        snmp = new Snmp(transport);
        transport.listen();
        CommandResponder trapPrinter = new CommandResponder() {
            public synchronized void processPdu(CommandResponderEvent e) {
                System.out.println("Received PDU...");
                PDU command = e.getPDU();
                if (command != null) {
                    System.out.println(command.toString());
                    ArrayList<String> trap = new ArrayList<>();
                    System.out.println(command.getVariableBindings().get(0).getVariable().getSyntaxString());
                    trap.add(command.getRequestID().toString());
                    trap.add(Integer.toString(command.getErrorIndex()));
                    trap.add(Integer.toString(command.getErrorStatus()));
                    trap.add(command.getVariableBindings().toString());
                    ViewerController.trapDataBase.add(trap);
                }
            }
        };
        snmp.addCommandResponder(trapPrinter);
    }



    public ArrayList<String> getAsString(OID oid) throws IOException {
        ResponseEvent event = get(new OID[]{oid});
        ArrayList<String> variable = new ArrayList<>();
        variable.add(event.getResponse().get(0).getOid().toString());
        variable.add(event.getResponse().get(0).getVariable().toString());
        variable.add(event.getResponse().get(0).getVariable().getSyntaxString());
        variable.add(address.substring(address.indexOf("1"),address.length()));

        return variable;
    }

    public String getOIDAsString(OID oid) throws IOException {
        ResponseEvent event = get(new OID[]{oid});
        return event.getResponse().get(0).getOid().toString();

    }

    public ArrayList<String> getNextAsString(OID oid) throws IOException {
        ResponseEvent event = getNext(new OID[]{oid});
        //System.err.println(event.getResponse().get(0).getVariable().getSyntaxString());
        ArrayList<String> variable = new ArrayList<>();
        variable.add(event.getResponse().get(0).getOid().toString());
        variable.add(event.getResponse().get(0).getVariable().toString());
        variable.add(event.getResponse().get(0).getVariable().getSyntaxString());
        variable.add(address.substring(address.indexOf("1"),address.length()));

        return variable;
    }

    public String getNextOIDAsString(OID oid) throws IOException {
        ResponseEvent event = getNext(new OID[]{oid});
        return event.getResponse().get(0).getOid().toString();
    }

    public void getAsString(OID oids,ResponseListener listener) {
        try {
            snmp.send(getPDU(new OID[]{oids}), getTarget(),null, listener);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private PDU getPDU(OID oids[]) {
        PDU pdu = new PDU();
        for (OID oid : oids) {
            pdu.add(new VariableBinding(oid));
        }

        pdu.setType(PDU.GET);
        return pdu;
    }

    private PDU getNextPDU(OID oids[]) {
        PDU pdu = new PDU();
        for (OID oid : oids) {
            pdu.add(new VariableBinding(oid));
        }

        pdu.setType(PDU.GETNEXT);
        return pdu;
    }

    public ResponseEvent get(OID oids[]) throws IOException {
        ResponseEvent event = snmp.send(getPDU(oids), getTarget(), null);
        if(event != null) {
            return event;
        }
        throw new RuntimeException("GET timed out");
    }

    public ResponseEvent getNext(OID oids[]) throws IOException {
        ResponseEvent event = snmp.send(getNextPDU(oids), getTarget(), null);
        if(event != null) {
            return event;
        }
        throw new RuntimeException("GETNEXT timed out");
    }

    private Target getTarget() {
        Address targetAddress = GenericAddress.parse(address);
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString("public"));
        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version2c);
        return target;
    }

    /**
     * Normally this would return domain objects or something else than this...
     */
    public List<List<String>> getTableAsStrings(OID[] oids) {
        rows = 0;
        TableUtils tUtils = new TableUtils(snmp, new DefaultPDUFactory());

        @SuppressWarnings("unchecked")
        List<TableEvent> events = tUtils.getTable(getTarget(), oids, null, null);
        List<List<String>> list = new ArrayList<>();
        for (TableEvent event : events) {
            System.out.println("Event...");
            if(event.getIndex().toString().startsWith("1.1.")){
                rows++;
            }

            if(event.isError()) {
                throw new RuntimeException(event.getErrorMessage());
            }
            List<String> strList = new ArrayList<>();
            list.add(strList);
            for(VariableBinding vb: event.getColumns()) {
                strList.add(vb.getVariable().toString());
                System.out.println("Dodaję: " + vb.getVariable().toString());
            }
        }
        return list;
    }

    public static String extractSingleString(ResponseEvent event) {
        return event.getResponse().get(0).getVariable().toString();
    }
}