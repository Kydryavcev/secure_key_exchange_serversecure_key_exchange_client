package ske.client;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import model.Client;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class ClientController
{
    /**
     * ОЖИДАНИЕ
     */
    private static final int WAITING = 1;

    private static final int SEND_MESSAGE = 2;
    private static final int MESSAGE_DELIVERED = 3;
    private static final int DISCONNECT = -1;

    private int SIGNAL = WAITING;
    private static final int ERROR_PORT_NUMBER = -1;

    private boolean connected = false;

    private Client client = new Client();

    @FXML
    private TextField portNumberTextField;

    @FXML
    private Button connectionButton, disconnectButton;

    @FXML
    private VBox messagesVBox;

    @FXML
    private Label errorValuePortNumberLabel, connectionEstablishedLabel, connectionNotSecureLabel, disconnectLabel;

    private javafx.event.EventHandler<WindowEvent> closeEventHandler =
            new javafx.event.EventHandler<WindowEvent>()
            {
                @Override
                public void handle(WindowEvent event)
                {
                   if (client != null)
                       client.disconnect();
                }
            };

    public javafx.event.EventHandler<WindowEvent> getCloseEventHandler()
    {
        return closeEventHandler;
    }

    @FXML
    public void onConnectionButton()
    {
        connectionButton.setDisable(true);
        portNumberTextField.setDisable(true);

        disconnectLabel.setVisible(false);
        connectionNotSecureLabel.setVisible(false);
        connectionEstablishedLabel.setVisible(false);

        Thread connection    = new Thread(new ConnectionClient());

        connection.setDaemon(true);

        connection.start();
    }

    @FXML
    protected void onClickDisconnectButton()
    {
        client.disconnect();

        connected = false;
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
        client.loadCertificate();

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
        if (connected)
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

                client.connectToSocket(portNumber);

                connectionEstablishedLabel.setVisible(true);

                disconnectButton.setDisable(false);

                client.connectionProtection();

                connected = true;

                Thread synchronization = new Thread(new Synchronization());

                synchronization.setDaemon(true);

                synchronization.start();
            }
            catch (Client.ClientInitializationException | Client.ConnectionProtectionException ex)
            {
                disconnectLabel.setVisible(true);

                disconnectButton.setDisable(true);
                portNumberTextField.setDisable(false);
                connectionButton.setDisable(!canConnect());

            }
        }
    }

    class Synchronization implements Runnable
    {
        @Override
        public void run()
        {
            try
            {

                exit:
                while (true)
                {
                    Thread.sleep(500);

                    int signal = DISCONNECT;

                    if (connected)
                        signal = client.synchronizationIn();

                    switch (signal)
                    {
                        case WAITING ->
                        {
//                            System.out.println("Ждём");
                        }
                        case SEND_MESSAGE ->
                        {
                            String message = client.getMessage();

                            if (message.length() == 0)
                                return;

                            Platform.runLater(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    Calendar calendar = new GregorianCalendar();

                                    String messageForLabel = String.format("%02d:%02d:%02d %s",
                                            calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE),
                                                                                calendar.get(Calendar.SECOND), message);

                                    Label messageLabel = new Label(messageForLabel);

                                    messagesVBox.getChildren().add(messageLabel);
                                }
                            });

                            SIGNAL = MESSAGE_DELIVERED;

                        }
                        case DISCONNECT ->
                        {
                            disconnect();

                            return;
                        }
                    }

                    switch (SIGNAL)
                    {
                        case WAITING ->
                        {
                            client.synchronizationOut(WAITING);
                        }
                        case MESSAGE_DELIVERED ->
                        {
                            client.synchronizationOut(MESSAGE_DELIVERED);

                            SIGNAL = WAITING;
                        }
                        case DISCONNECT ->
                        {
                            disconnect();

                            return;
                        }
                    }
                }
            }
            catch (Client.SynchronizationException ex)
            {
                System.out.println(ex.getMessage());

                disconnect();
            }
            catch (Client.GetMessageException ex)
            {
                System.out.println(ex.getMessage());

                disconnect();
            }
            catch (InterruptedException ex)
            {
                System.out.println(ex.getMessage());
            }
        }

        private void disconnect()
        {
            connected = false;
            disconnectLabel.setVisible(true);
            connectionEstablishedLabel.setVisible(false);

            disconnectButton.setDisable(true);
            portNumberTextField.setDisable(false);
            connectionButton.setDisable(!canConnect());
        }
    }
}
