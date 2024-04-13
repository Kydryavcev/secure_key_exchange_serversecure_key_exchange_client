package model;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class Client
{
    private Socket socket;

    private DataInputStream in;
    private DataOutputStream out;

    private Key secretKey;

    private Signature signature;

    public void loadCertificate()
    {
        try
        {
            Certificate certificate = CryptographicAlgorithms.getCertificate();

            signature = Signature.getInstance("MD5withRSA");

            signature.initVerify(certificate);
        }
        catch (NoSuchAlgorithmException|InvalidKeyException ex)
        {
            try (FileWriter fw = new FileWriter("src/main/resources/logs/.log"))
            {
                fw.write(java.time.LocalDateTime.now().toString());
                fw.write(CryptographicAlgorithms.class.getName() + "\n");
                fw.write(new Exception().getStackTrace()[0].getMethodName() + "\n");
                fw.write(ex.getClass().getName() + "\n");
                fw.write(ex.getMessage() + "\n");
            }
            catch (IOException ex1)
            {
                System.out.println("Файла для логирования не существует");
                System.out.println(ex1.getMessage());
            }
        }
    }

    public void connectToSocket(int port) throws ClientInitializationException
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

            if (secretKey == null)
                throw new NullPointerException("В ходе генерации секретного ключа произошла ошибка.");

            byte[] wrapSK = CryptographicAlgorithms.wrapKey(secretKey, publicKey);

            if (wrapSK == null)
                throw new NullPointerException("В ходе свёртки секретного ключа произошла ошибка.");

            out.write(wrapSK);
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
        catch (NullPointerException ex)
        {
            throw new ConnectionProtectionException(ex.getMessage());
        }
    }

    public String getMessage() throws GetMessageException
    {
        try
        {
            if (secretKey == null)
                throw new GetMessageException("Соединение не защищено.");

            byte[] cipherBytes = new byte[100], buffer = new byte[100];

            int cipherBytesLength = in.read(cipherBytes);

            while (in.available() > 0)
            {
                cipherBytesLength += in.read(buffer);

                byte[] temp = new byte[cipherBytesLength];

                System.arraycopy(cipherBytes, 0, temp, 0, cipherBytes.length);
                System.arraycopy(buffer, 0, temp, cipherBytes.length,
                        cipherBytesLength - cipherBytes.length);

                cipherBytes = temp;
            }

//            System.out.println(finalMessageBytesLength);

            String message = "";

            if (cipherBytesLength <= 0)
                return message;

            byte[] finalMessageBytes = CryptographicAlgorithms.decrypt(cipherBytes,cipherBytesLength, secretKey);

            if (finalMessageBytes == null)
                throw new NullPointerException("В ходе расшифрования сообщения произошла ошибка.");

            int finalMessageBytesLength = finalMessageBytes.length;

//            System.out.println("Length finalMessageBytes: " + finalMessageBytesLength);

            byte[] lengthMessageBytes = new byte[4];

            System.arraycopy(finalMessageBytes, 0,lengthMessageBytes,0, 4);

            int lengthMessage = bsToInt(lengthMessageBytes);

//            System.out.println("Длина в байтах: " + lengthMessage);

            byte[] messageBytes = new byte[lengthMessage];

            System.arraycopy(finalMessageBytes,4,messageBytes,0,lengthMessage);

            signature.update(messageBytes);

            byte[] sign = new byte[finalMessageBytesLength - lengthMessage - 4];

//            System.out.println("Length sing: " + sign.length);

            if (signature.verify(finalMessageBytes, lengthMessage + 4, finalMessageBytesLength - lengthMessage - 4))
                System.out.println("Сообщение подтверждено ЭЦП");
            else
                System.out.println("Сообщение не подтверждено ЭЦП");

            message = new String(messageBytes);


            return message;
        }
        catch (IOException ex)
        {
            throw new GetMessageException("Возникла ошибка ввода-вывода.");
        }
        catch (java.security.SignatureException ex)
        {
            throw new GetMessageException("Класс формирования ЭЦП не инициализирован должным образом. Возможно сообщение " +
                    "подписано не тем ключом.");
        }
        catch (NullPointerException ex)
        {
            throw new GetMessageException(ex.getMessage());
        }
    }


    public int synchronizationIn() throws SynchronizationException
    {
        int result = - 1;

        try
        {
            if (in.available() > 0)
            {
                result = in.read();
            }
        }
        catch (IOException ex)
        {
            throw new SynchronizationException("Возникла ошибка ввода-вывода.");
        }

        return result;
    }

    public void synchronizationOut(int signal) throws SynchronizationException
    {
        try
        {
            out.write(signal);
        }
        catch (IOException ex)
        {
            throw new SynchronizationException("Возникла ошибка ввода-вывода.");
        }
    }

    /**
     * <h2>Потерять соединение</h2>
     */
    public void disconnect()
    {
        try
        {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        }
        catch (IOException ex)
        {
            try (FileWriter fw = new FileWriter("src/main/resources/logs/.log"))
            {
                fw.write(java.time.LocalDateTime.now().toString());
                fw.write(CryptographicAlgorithms.class.getName() + "\n");
                fw.write(new Exception().getStackTrace()[0].getMethodName() + "\n");
                fw.write(ex.getClass().getName() + "\n");
                fw.write(ex.getMessage() + "\n");
            }
            catch (IOException ex1)
            {
                System.out.println("Файла для логирования не существует");
                System.out.println(ex1.getMessage());
            }
        }
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

    public class SynchronizationException extends Exception
    {
        public SynchronizationException(String message)
        {
            super(message);
        }
    }

    /**
     * <h2>Преобразование массива байтов в число типа данных int.</h2>
     */
    private int bsToInt(byte[] bytes)
    {
        int num = 0;

        int shift = 0;

        for (int i = 0; i < 4; i++)
        {
            byte b = bytes[i];

            num |= b << shift;

            shift += 4;
        }

        return num;
    }
}
