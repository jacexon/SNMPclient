import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;

/**
 * Główna klasa programu klienta SNMP, w niej uruchamiany jest interfejs graficzny użytkownika.
 * @see "http://www.snmp4j.org/doc/index.html"
 * @author Jacek Polak
 * @author Bartłomiej Bielecki
 * @author Wojciech Darowski
 * @since 22.11.2016
 * @version 3.1
 */
public class Main extends Application {

    /**
     * Funkcja uruchamiająca program.
     * @throws IOException
     */
    public static final void main(String args[]) throws IOException {
        launch(args);
    }


    /**
     * Metoda ładująca GUI z pliku XML'owego.
     * @param primaryStage
     * @throws Exception
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("Viewer.fxml"));
        primaryStage.setTitle("SNMP Client");
        primaryStage.setScene(new Scene(root, 715, 509));
        primaryStage.show();
        primaryStage.setOnCloseRequest(t -> {
            Platform.exit();
            System.exit(0);
        });
        primaryStage.setMinHeight(600);
        primaryStage.setMinWidth(640);
        String musicFile = "moonlightsonata.mp3";

        Media sound = new Media(new File(musicFile).toURI().toString());
        MediaPlayer mediaPlayer = new MediaPlayer(sound);
        mediaPlayer.play();



    }
}