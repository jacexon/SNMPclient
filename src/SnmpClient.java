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

/**
 * Klasa definiująca klienta SNMP wraz z całą jego funkcjonalnością.
 */
public class SnmpClient {

    private String address;
    private Snmp snmp;
    public int rows;

    /**
     * Konstruktor klasy klienta SNMP, który za parametr wejściowy przyjmuje adres sieciowy, za pomocą którego
     * będzie komunikował się z agentem Windowsowym.
     * @param address
     */
    public SnmpClient(String address) {
        super();
        this.address = address;
        try {
            start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Metoda rozpoczynająca faktyczne działanie klienta SNMP; rozpoczyna nasłuchiwanie na powiadomienia TRAP.
     * @throws IOException
     */
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


    /**
     * Metoda reprezentująca operację SNMP "getRequest", zwracająca rezultat tej operacji w postaci obiektu klasy
     * ArrayList<String>
     * @param oid Obiekt klasy OID, który chcemy wyświetlić
     * @return Lista przechowująca informacje o zwracanym obiekcie
     * @throws IOException
     */
    public ArrayList<String> getAsString(OID oid) throws IOException {
        ResponseEvent event = get(new OID[]{oid});
        ArrayList<String> variable = new ArrayList<>();
        variable.add(event.getResponse().get(0).getOid().toString());
        variable.add(event.getResponse().get(0).getVariable().toString());
        variable.add(event.getResponse().get(0).getVariable().getSyntaxString());
        variable.add(address.substring(address.indexOf("1"),address.length()));

        return variable;
    }

    /**
     * Metoda reprezentująca operację SNMP "getNextRequest", zwracająca rezultat tej operacji w postaci obiektu klasy
     * ArrayList<String>
     * @param oid Obiekt klasy OID, który chcemy wyświetlić
     * @return Lista przechowująca informacje o zwracanym obiekcie
     * @throws IOException
     */
    public ArrayList<String> getNextAsString(OID oid) throws IOException {
        ResponseEvent event = getNext(new OID[]{oid});
        ArrayList<String> variable = new ArrayList<>();
        variable.add(event.getResponse().get(0).getOid().toString());
        variable.add(event.getResponse().get(0).getVariable().toString());
        variable.add(event.getResponse().get(0).getVariable().getSyntaxString());
        variable.add(address.substring(address.indexOf("1"),address.length()));

        return variable;
    }

    /**
     * Metoda zwracająca PDU, którego OID podajemy w parametrze wywołania. (operacja getRequest)
     * @param oids
     * @return PDU
     */
    private PDU getPDU(OID oids[]) {
        PDU pdu = new PDU();
        for (OID oid : oids) {
            pdu.add(new VariableBinding(oid));
        }

        pdu.setType(PDU.GET);
        return pdu;
    }

    /**
     * Metoda zwracająca następne PDU po tym, którego OID podajemy w parametrze wywołania. (operacja getNextRequest)
     * @param oids
     * @return PDU
     */
    private PDU getNextPDU(OID oids[]) {
        PDU pdu = new PDU();
        for (OID oid : oids) {
            pdu.add(new VariableBinding(oid));
        }

        pdu.setType(PDU.GETNEXT);
        return pdu;
    }

    /**
     * Metoda zwracająca zdarzenie, za pomocą którego będziemy uzyskiwać informacje o obiektach. (getRequest)
     * @param oids Tablica OID obiektów, które chcemy przetwarzać
     * @return ResponseEvent
     * @throws IOException
     */
    public ResponseEvent get(OID oids[]) throws IOException {
        ResponseEvent event = snmp.send(getPDU(oids), getTarget(), null);
        if(event != null) {
            return event;
        }
        throw new RuntimeException("GET timed out");
    }

    /**
     * Metoda zwracająca zdarzenie, za pomocą którego będziemy uzyskiwać informacje o obiektach. (getNextRequest)
     * @param oids Tablica OID obiektów, które chcemy przetwarzać
     * @return ResponseEvent
     * @throws IOException
     */
    public ResponseEvent getNext(OID oids[]) throws IOException {
        ResponseEvent event = snmp.send(getNextPDU(oids), getTarget(), null);
        if(event != null) {
            return event;
        }
        throw new RuntimeException("GET NEXT timed out");
    }

    /**
     * Metoda definiująca parametry "sesji" SNMP tj. wersja (2c), adres sieciowy, czas do timeoutu
     * @return Obiekt klasy Target
     */
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
     * Metoda służąca do obsługiwania tabel z agenta SNMP.
     * @param oids Tablica OID obiektów, które chcemy uzyskać; zwracane w postaci Stringów
     * @return Struktura zawierająca wszystkie pola tabeli.
     */
    public List<List<String>> getTableAsStrings(OID[] oids) {
        rows = 0;
        TableUtils tUtils = new TableUtils(snmp, new DefaultPDUFactory());

        @SuppressWarnings("unchecked")
        List<TableEvent> events = tUtils.getTable(getTarget(), oids, null, null);
        List<List<String>> list = new ArrayList<>();
        for (TableEvent event : events) {
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
            }
        }
        return list;
    }
}