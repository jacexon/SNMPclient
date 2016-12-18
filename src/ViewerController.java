import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import org.ietf.jgss.Oid;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.io.File;
import javax.swing.table.*;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Klasa kontrolera graficznego interfejsu użytkownika.
 * @author Jacek Polak
 * @author Bartłomiej Bielecki
 * @author Wojciech Darowski
 * @since 22.11.2016
 * @version 3.1
 */
public class ViewerController implements Initializable {

    //region COMPONENTS
    @FXML
    private TextField oidText;
    @FXML
    private TextField monitorOidText;
    @FXML
    private MenuButton commands;
    @FXML
    private MenuItem get_menuitem;
    @FXML
    private MenuItem getNext_menuitem;
    @FXML
    private MenuItem getTable_menuitem;
    @FXML
    private TableView<ObservableList> resultTable;
    @FXML
    private TableView<ObservableList> trapTable;
    @FXML
    private TableView<ObservableList> monitorResultTable;
    @FXML
    private Button exitButton;


    private String OIDstring;
    private SnmpClient client;
    private ArrayList<String> colsNames = new ArrayList<>();
    public static ArrayList<ArrayList<String>> trapDataBase = new ArrayList<>();
    public ArrayList<ArrayList<String>> monitorDataBase = new ArrayList<>();
    public ArrayList<OID> oidBase = new ArrayList<>();
    //endregion


    /**
     * Metoda wykonująca się przy załadowaniu pliku XML'owego. Dodaje kolumny do tabel, rozpoczyna pracę z agentem
     * Windowsowym SNMP, uruchamia wątki nasłuchujące.
     * @param location
     * @param resources
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        addTrapColumns();
        client = new SnmpClient("udp:127.0.0.1/161");
        OIDstring = new String("");
        colsNames.add("Name/OID");
        colsNames.add("Value");
        colsNames.add("Type");
        colsNames.add("IP:Port");
        new Thread(run).start();
        new Thread(run2).start();

    }

    Runnable run = new Runnable() {
        @Override
        public void run() {
            while (true) {
                if (!trapDataBase.isEmpty()) {
                    ObservableList<String> ro;
                    ObservableList<ObservableList> data = FXCollections.observableArrayList();
                    for (ArrayList<String> ar : trapDataBase) {
                        ro = FXCollections.observableArrayList();
                        for (int i = 0; i < 4; i++) {
                            ro.add(ar.get(i));
                        }
                        data.add(ro);
                    }
                    Platform.runLater(() -> {
                        trapTable.setItems(data);
                    });
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    /**
     * Akcja wykonująca się po naciśnięciu przycisku "Exit"
     * @param event
     */
    public void exitButtonAction(ActionEvent event){
        System.exit(0);
    }

    /**
     * Metoda dodająca kolumny do tabeli, w których będą pojawiać się powiadomienia TRAP, a także do tabeli
     * monitora.
     */
    public void addTrapColumns() {
        ArrayList<String> trapNames = new ArrayList<>();
        trapNames.add("requestID");
        trapNames.add("errorStatus");
        trapNames.add("errorIndex");
        trapNames.add("varBindings");

        for (int i = 0; i <= 3; i++) {
            final int j = i;
            TableColumn col = new TableColumn(trapNames.get(i));
            col.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ObservableList, String>, ObservableValue<String>>() {
                public ObservableValue<String> call(TableColumn.CellDataFeatures<ObservableList, String> param) {
                    return new SimpleStringProperty(param.getValue().get(j).toString());
                }
            });
            trapTable.getColumns().addAll(col);
        }

        ArrayList<String> monitorNames = new ArrayList<>();
        monitorNames.add("OID");
        monitorNames.add("Value");
        monitorNames.add("Type");
        monitorNames.add("IP:Port");
        for (int i = 0; i < 4; i++) {
            final int j = i;
            TableColumn col = new TableColumn(monitorNames.get(i));
            col.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ObservableList, String>, ObservableValue<String>>() {
                public ObservableValue<String> call(TableColumn.CellDataFeatures<ObservableList, String> param) {
                    return new SimpleStringProperty(param.getValue().get(j).toString());
                }
            });
            monitorResultTable.getColumns().addAll(col);
        }
    }

    /**
     * Metoda odczytująca OID jako obiekt klasy String z okna.
     * @param action
     * @throws Exception
     */
    public void readOID(ActionEvent action) throws Exception {
        OIDstring = oidText.getText();
    }

    /**
     * Metoda służąca do wykonania operacji GetRequest.
     * @throws IOException
     */
    public void getAction() throws IOException {
        resultTable.getColumns().clear();
        OIDstring = oidText.getText();
        ArrayList<String> sysDescr = client.getAsString(new OID(OIDstring));
        commands.setText(get_menuitem.getText());
        System.out.println(sysDescr);

        for (int i = 0; i < 4; i++) {
            final int j = i;
            TableColumn col = new TableColumn(colsNames.get(i));
            col.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ObservableList, String>, ObservableValue<String>>() {
                public ObservableValue<String> call(TableColumn.CellDataFeatures<ObservableList, String> param) {
                    return new SimpleStringProperty(param.getValue().get(j).toString());
                }
            });
            resultTable.getColumns().addAll(col);
        }
        ObservableList<ObservableList> data = FXCollections.observableArrayList();
        ObservableList<String> rows = FXCollections.observableArrayList();

        for (int j = 0; j < 4; j++) {
            rows.add(sysDescr.get(j));
        }
        data.add(rows);
        resultTable.setItems(data);
    }

    /**
     * Metoda służąca do wykonania operacji GetNextRequest.
     * @throws IOException
     */
    public void getNextAction() throws IOException {
        resultTable.getColumns().clear();
        OIDstring = oidText.getText();
        ArrayList<String> sysDescr = client.getNextAsString(new OID(OIDstring));
        commands.setText(getNext_menuitem.getText());

        for (int i = 0; i < 4; i++) {
            final int j = i;
            TableColumn col = new TableColumn(colsNames.get(i));
            col.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ObservableList, String>, ObservableValue<String>>() {
                public ObservableValue<String> call(TableColumn.CellDataFeatures<ObservableList, String> param) {
                    return new SimpleStringProperty(param.getValue().get(j).toString());
                }
            });
            resultTable.getColumns().addAll(col);
        }
        ObservableList<ObservableList> data = FXCollections.observableArrayList();
        ObservableList<String> rows = FXCollections.observableArrayList();

        for (int j = 0; j < 4; j++) {
            rows.add(sysDescr.get(j));
        }
        data.add(rows);
        resultTable.setItems(data);
    }

    /**
     * Metoda służąca do wykonania operacji GetRequest i zaprezentowaniu wyników w postaci tabeli.
     * @throws IOException
     */
    public void getTableAction() throws IOException {
        resultTable.getColumns().clear();
        OIDstring = oidText.getText();
        ArrayList<String> sysDescr = client.getAsString(new OID(OIDstring));
        commands.setText(getTable_menuitem.getText());
        System.out.println(sysDescr);
        createTable();
    }

    /**
     * Metoda tworząca tabelę służącą do przedstawienia wyników operacji getTable.
     * @throws IOException
     */
    private void createTable() throws IOException {
        String tableOID = OIDstring;
        ObservableList<ObservableList> data = null;
        List<List<String>> table = client.getTableAsStrings(new OID[]{new OID(tableOID)});
        int rowsNumber = client.rows;
        if (rowsNumber == 0) return;
        int all = 0;
        for (List<String> list : table) {
            all += list.size();
        }
        data = FXCollections.observableArrayList();
        for (int i = 0; i < all / rowsNumber; i++) {
            final int j = i;
            TableColumn col = new TableColumn("elo" + i);
            col.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ObservableList, String>, ObservableValue<String>>() {
                public ObservableValue<String> call(TableColumn.CellDataFeatures<ObservableList, String> param) {
                    return new SimpleStringProperty(param.getValue().get(j).toString());
                }
            });
            resultTable.getColumns().addAll(col);
        }

        for (int z = 0; z < rowsNumber; z++) {
            int j = z;
            ObservableList<String> rows = FXCollections.observableArrayList();
            while (j < all) {
                rows.add(table.get(j).get(0));
                System.out.println(table.get(j).get(0));
                j = j + rowsNumber;
            }
            data.add(rows);
        }
        resultTable.setItems(data);
    }


    /**
     * Metoda dodająca obiekt o zadanym OID do monitora.
     * @param event
     * @throws IOException
     */
    public void addToMonitor(ActionEvent event) throws IOException{
        if (!monitorOidText.getText().equals("")) {
            if(oidBase.size() == 0)
                oidBase.add(new OID(monitorOidText.getText()));
            else
                oidBase.set(0,new OID(monitorOidText.getText()));
        }
    }
    Runnable run2 = new Runnable() {
        @Override
        public void run() {
            while (true) {
                try {
                    //monitorDataBase.add(client.getAsString(new OID(monitorOidText.getText())));
                    for (OID o : oidBase){
                        monitorDataBase.add(client.getAsString(o));
                    }
                    ObservableList<String> rows;
                    ObservableList<ObservableList> data = FXCollections.observableArrayList();
                    for (ArrayList<String> ad : monitorDataBase) {
                        rows = FXCollections.observableArrayList();
                        for (int i = 0; i < 4; i++) {
                            rows.add(ad.get(i));
                        }
                        data.add(rows);
                    }

                    Platform.runLater(() -> {
                        monitorResultTable.setItems(data);
                    });
                    Thread.sleep(3000);
                    monitorDataBase.clear();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };
}
