package ske.client;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.Client;

public class ClientController
{
    public static final int ERROR_PORT_NUMBER = -1;

    private Client client;

    @FXML
    private TextField portNumberTextField;

    @FXML
    private Button connectionButton, disconnectButton;

    @FXML
    private Label errorValuePortNumberLabel, connectionEstablishedLabel, connectionNotSecureLabel, disconnectLabel,
            messageLabel;

    @FXML
    public void onConnectionButton()
    {
        connectionButton.setDisable(true);
        portNumberTextField.setDisable(true);

        connectionNotSecureLabel.setVisible(false);
        connectionEstablishedLabel.setVisible(false);

        Thread connection    = new Thread(new ConnectionClient());
        Thread searchMessage = new Thread(new SearchMessage());
        Thread isConnected   = new Thread(new IsConnected());

        connection.setDaemon(true);
        isConnected.setDaemon(true);
        searchMessage.setDaemon(true);

        connection.start();
        isConnected.start();
        searchMessage.start();
    }

    /**
     * <h3>Инициализация контроллера.</h3>
     *
     * <p>Добавляет прослушивателя к полю ввода номера порта. В подключаемом прослушивателе происходит волидация вводимой
     * строки.</p>
     *
     * <p><i>Nota bene</i>: метод инициализации имеет доступ к объектам интерфейса FXML, поэтому использование конструктора
     * класса невозможно в подобных ситуациях.</p>
     */
    @FXML
    public void initialize()
    {
        portNumberTextField.textProperty().addListener(new ChangeListener<String>()
        {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
            {
                if (!newValue.matches("[^0]\\d{1,4}|\\d?"))
                {
                    portNumberTextField.setText(oldValue);
                }

                boolean visible = (getPortNumberTextFieldValue() == ERROR_PORT_NUMBER);

                errorValuePortNumberLabel.setVisible(visible);

                connectionButton.setDisable(!canConnect());
            }
        });
    }

    /**
     * <h3>Возвращает численное значения поля ввода номера порта</h3>
     * @return номер порта или {@code Server.ERROR_PORT_NUMBER}
     */
    private int getPortNumberTextFieldValue()
    {
        String portNumberTextFieldValue = portNumberTextField.getText();

        try
        {
            int portNumber = Integer.parseInt(portNumberTextFieldValue);

            if (portNumber < 0 || portNumber > 65535)
                new NumberFormatException();

            return portNumber;
        }
        catch (NumberFormatException ex)
        {
            return ERROR_PORT_NUMBER;
        }
    }

    /**
     * <h3>Проверка возможности соединения</h3>
     *
     * @return если соединение возможно или необходимо восстановить, то {@code True}, иначе {@code False}
     */
    private boolean canConnect()
    {
        if (client != null && client.isConnect())
            return false;

        return getPortNumberTextFieldValue() != ERROR_PORT_NUMBER;
    }

    class ConnectionClient implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                int portNumber = getPortNumberTextFieldValue();

                client = new Client(portNumber);

                connectionEstablishedLabel.setVisible(true);

                client.connectionProtection();
            }
            catch (Client.ClientInitializationException ex)
            {

            }
            catch (Client.ConnectionProtectionException ex)
            {

            }
        }
    }

    class IsConnected implements Runnable
    {
        @Override
        public void run()
        {
            while (true)
            {
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException ex)
                {

                }

                if ( client != null && !client.isConnect())
                {
                    Platform.runLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            disconnectLabel.setVisible(true);

                            connectionEstablishedLabel.setVisible(false);
                            connectionButton.setDisable(false);
                            portNumberTextField.setDisable(false);
                        }
                    });
                }
            }
        }
    }

    class SearchMessage implements Runnable
    {
        @Override
        public void run()
        {
            while (true)
            {
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException ex)
                {

                }

                String message;

                try
                {
                    message = client.getMessage();
                }
                catch (Client.GetMessageException ex)
                {
                    System.out.println(ex.getMessage());

                    continue;
                }

                if (message.length() == 0)
                    return;

                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        messageLabel.setText(message);

                        messageLabel.setVisible(true);
                    }
                });
            }
        }
    }
}
