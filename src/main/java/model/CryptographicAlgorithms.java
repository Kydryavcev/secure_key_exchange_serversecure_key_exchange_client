package model;

import javax.crypto.*;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

/**
 * <h2>Класс криптографических алгоритмов</h2>
 *
 * <p>В этом классе реализованы криптоалгоритмы, необходимые для создания защищенного соединения и шифрования сообщения.</p>
 *
 * @author Kydryavcev Ilya
 * @version 1.0
 * @since 12.03.24
 */
public class CryptographicAlgorithms
{
    /**
     * <h2>Генерация секретного ключа для алгоритма AES.</h2>
     *
     * @return Секретный ключ или {@code null}, если произошла ошибка (см. файл .log).
     */
    public static SecretKey generateKey()
    {
        try
        {
            SecureRandom sr = new SecureRandom();

            KeyGenerator generator = KeyGenerator.getInstance("AES");

            generator.init(sr);

            return generator.generateKey();
        }
        catch (NoSuchAlgorithmException ex)
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

        return null;
    }

    /**
     * <h3>Свёртка симметричного ключа</h3>
     *
     * <p>Данный метод сворачивает ключ {@param wrappedKey} с помощью открытого ключа {@code key}</p>
     *
     * @param secretKey экспортируемый ключ, который будет подвержен свёртке.
     * @param key открытый ключ, с помощью которого будет сворачиваться секретный ключ {@code secretKey}.
     *
     * @return Cекретный ключ в виде байтового массива или {@code null}, если произошла ошибка (см. файл .log).
     */
    public static byte[] wrapKey(Key secretKey, PublicKey key)
    {
        try
        {
            Cipher cipher = Cipher.getInstance("RSA");

            cipher.init(Cipher.WRAP_MODE, key);

            return cipher.wrap(secretKey);
        }
        catch (NoSuchAlgorithmException|NoSuchPaddingException|InvalidKeyException|IllegalBlockSizeException ex)
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

        return null;
    }

    /**
     * <h3>Расшифрование данных</h3>
     *
     * <p>Расшифровывает данные представленные параметром {@code data}, ключом {@code key}. Расшифрование происходит по
     * алгоритму AES.</p>
     *
     * @param data массив байтов, представляющий данные.
     * @param length количество байт в массиве, представляющие данные.
     * @param key ключ, с помощью которого будет произведено расшифрование.
     *
     * @return Массив байтов, представляющий сабой расшифрованные данные или {@code null},
     * если произошла ошибка (см. файл .log).
     */
    public static byte[] decrypt(byte[] data, int length, Key key)
    {
        try
        {
            Cipher cipher = Cipher.getInstance("AES");

            cipher.init(Cipher.DECRYPT_MODE, key);

            return cipher.doFinal(data, 0, length);
        }
        catch (NoSuchAlgorithmException|NoSuchPaddingException|InvalidKeyException|
               IllegalBlockSizeException|BadPaddingException ex)
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

        return null;
    }

    /**
     * <h2>Чтение из файла сертификата открытого ключа сервера</h2>
     *
     * @return Сертификат открытого ключа сервера или {@code null}, если произошла ошибка (см. файл .log).
     */
    public static Certificate getCertificate()
    {

        try (FileInputStream fis = new FileInputStream("src/main/resources/certificates/skp.cer"))
        {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            return cf.generateCertificate(fis);
        }
        catch (IOException|CertificateException ex)
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

        return null;
    }
}
