package model;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class Client
{
    private Socket socket;

    private DataInputStream in;
    private DataOutputStream out;

    private Key secretKey;

    public Client(int port) throws ClientInitializationException
    {
        try
        {
            socket = new Socket("localhost", port);

            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        }
        catch (UnknownHostException ex)
        {
            throw new ClientInitializationException("Задан не определённый хост.");
        }
        catch (IOException ex)
        {
            throw new ClientInitializationException("Произошла ошибка ввода-вывода.");
        }
    }

    /**
     * <h3>Установка соединения в защищенное состояние</h3>
     *
     * @throws ConnectionProtectionException если по каким-то причинам не получилось защитить канал передачи данных
     */
    public void connectionProtection()
            throws ConnectionProtectionException
    {
        if (socket == null)
            throw new ConnectionProtectionException("Нет связи с сервер-приложением.");

        try
        {
            byte[] publicKeyBytes = new byte[100], buffer = new byte[100];

            int lengthMessage = in.read(publicKeyBytes);

            while (in.available() > 0)
            {
                lengthMessage += in.read(buffer);

                byte[] temp = new byte[lengthMessage];

                System.arraycopy(publicKeyBytes, 0, temp,0, publicKeyBytes.length);
                System.arraycopy(buffer,0,temp,publicKeyBytes.length, lengthMessage - publicKeyBytes.length);

                publicKeyBytes = temp;
            }

            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            secretKey = CryptographicAlgorithms.generateKey();

            out.write(CryptographicAlgorithms.wrapKey(secretKey, publicKey));
        }
        catch (InvalidKeySpecException ex)
        {
            throw new ConnectionProtectionException("Данная спецификация ключа не подходит для этой фабрики ключей " +
                    "для создания открытого ключа.");
        }
        catch (IOException ex)
        {
            throw new ConnectionProtectionException("Возникла ошибка ввода-вывода.");
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new ConnectionProtectionException("Провайдер не поддерживает реализация криптоалгоритма.");
        }
        catch (NoSuchPaddingException ex)
        {
            throw new ConnectionProtectionException("Криптографический механизм загружен и не доступен.");
        }
        catch (InvalidKeyException ex)
        {
            throw new ConnectionProtectionException("Установленный ключ не поддерживается для данного алгоритма.");
        }
        catch (IllegalBlockSizeException ex)
        {
            throw new ConnectionProtectionException("Некорректная длина блока");
        }
    }

    public String getMessage() throws GetMessageException
    {
        try
        {
            if (secretKey == null)
                throw new GetMessageException("Соединение не защищено.");

            byte[] messageBytes = new byte[100], buffer = new byte[100];

            int messageBytesLength = in.read(messageBytes);

            while (in.available() > 0)
            {
                messageBytesLength += in.read(buffer);

                byte[] temp = new byte[messageBytesLength];

                System.arraycopy(messageBytes, 0, temp, 0, messageBytes.length);
                System.arraycopy(buffer, 0, temp, messageBytes.length,
                        messageBytesLength - messageBytes.length);

                messageBytes = temp;
            }

            String message = "";

            if (messageBytesLength == 0)
                return message;


            try
            {
                message = new String(CryptographicAlgorithms.decrypt(messageBytes,messageBytesLength, secretKey));
            }
            catch (NoSuchAlgorithmException ex)
            {
                throw new GetMessageException("Произошли ошибки в работе алгоритма.");
            }
            catch (NoSuchPaddingException ex)
            {
                throw new GetMessageException("Преобразование содержит схему заполнения, которая недоступна.");
            }
            catch (InvalidKeyException ex)
            {
                throw new GetMessageException("Данный ключ не подходит для инициализации этого шифра.");
            }
            catch (IllegalBlockSizeException ex)
            {
                throw new GetMessageException("Длина сообщения не соответствует длине блока.");
            }
            catch (BadPaddingException ex)
            {
                throw new GetMessageException("Сообщение дополнено неверным образом.");
            }

            return message;
        }
        catch (IOException ex)
        {
            throw new GetMessageException("Возникла ошибка ввода-вывода.");
        }
    }

    /**
     * <h2>Проверка соединения</h2>
     * @return
     */
    public boolean isConnect()
    {
        return socket != null;
    }

    public boolean connected()
    {
        return true; // Необходимо постоянно обмениваться сигналами, чтобы знать состояние соединения. При этом
                     // необходимо синхронизировать сообщения проверки соединения и приемки сообщений от сервера.
    }

    public class ClientInitializationException extends Exception
    {
        public ClientInitializationException(String message)
        {
            super(message);
        }
    }

    public class ConnectionProtectionException extends Exception
    {
        public ConnectionProtectionException(String message)
        {
            super(message);
        }
    }

    public class GetMessageException extends Exception
    {
        public GetMessageException(String message)
        {
            super(message);
        }
    }
}
