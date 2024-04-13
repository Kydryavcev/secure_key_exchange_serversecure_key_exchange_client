package ske.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;

public class ClientApplication extends Application
{
    @Override
    public void start(Stage stage) throws IOException
    {
        FXMLLoader fxmlLoader = new FXMLLoader(ClientApplication.class.getResource("client-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 520);

        stage.setMinWidth(600);
        stage.setMinHeight(400);
        stage.setTitle("Приложение клиента");
        InputStream inIcon = ClientApplication.class.getResourceAsStream("/image/client.png");
        stage.getIcons().add(new Image(inIcon));
        stage.setScene(scene);
        stage.show();

        ClientController controller = fxmlLoader.getController();

        stage.setOnCloseRequest(controller.getCloseEventHandler());
    }

    public static void main(String[] args)
    {
        launch();
    }

}
