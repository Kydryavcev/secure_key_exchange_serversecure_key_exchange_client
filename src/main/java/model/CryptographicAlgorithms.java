package model;

import javax.crypto.*;
import java.security.*;

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
    public static SecretKey generateKey() throws NoSuchAlgorithmException
    {
        SecureRandom sr = new SecureRandom();

        KeyGenerator generator = KeyGenerator.getInstance("AES");

        generator.init(sr);

        return generator.generateKey();
    }

    /**
     * <h3>Свёртка симметричного ключа</h3>
     *
     * <p>Данный метод сворачивает ключ {@param wrappedKey} с помощью открытого ключа {@code key}</p>
     *
     * @throws NoSuchAlgorithmException если преобразование имеет значение null, пусто, имеет недопустимый формат или
     * если ни один поставщик не поддерживает реализацию CipherSpi для указанного алгоритма.
     * @throws NoSuchPaddingException если преобразование содержит схему заполнения, которая недоступна.
     * @throws InvalidKeyException если данный ключ не подходит для инициализации этого шифра или требует параметров
     * алгоритма, которые не могут быть определены из данного ключа, или если данный ключ имеет размер ключа, который
     * превышает максимально допустимый размер ключа (как определено из настроенных файлов политики юрисдикции).
     * @throws IllegalBlockSizeException если этот шифр является блочным, заполнение не запрашивалось, а длина кодировки
     * ключа, подлежащего переносу, не кратна размеру блока
     *
     * @param secretKey экспортируемый ключ, который будет подвержен свёртке.
     * @param key открытый ключ, с помощью которого будет сворачиваться секретный ключ {@code secretKey}.
     *
     * @return секретный ключ.
     */
    public static byte[] wrapKey(Key secretKey, PublicKey key)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException
    {
        Cipher cipher = Cipher.getInstance("RSA");

        cipher.init(Cipher.WRAP_MODE, key);

        return cipher.wrap(secretKey);
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
     * @return Массив байтов, представляющий сабой расшифрованные данные.
     *
     * @throws NoSuchAlgorithmException если преобразование имеет нулевое значение, пусто, имеет недопустимый формат или,
     * если ни один провайдер не поддерживает реализацию CipherSpi для указанного алгоритма.
     * @throws NoSuchPaddingException если преобразование содержит схему заполнения, которая недоступна.
     * @throws InvalidKeyException если данный ключ не подходит для инициализации этого шифра или, если данный ключ имеет
     * размер ключа, который превышает максимально допустимый размер ключа.
     * @throws IllegalBlockSizeException если этот шифр является блочным, заполнение не запрашивалось (только в режиме
     * шифрования), а общая входная длина данных, обработанных этим шифром, не кратна размеру блока; или если этот
     * алгоритм шифрования не может обработать предоставленные входные данные.
     * @throws BadPaddingException если при расшифровании с отсечением дополнительных байтов содержимое дополнительного
     * байта не соответствует количеству байтов, подлежащих отсечению
     */
    public static byte[] decrypt(byte[] data, int length, Key key)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException
    {
        Cipher cipher = Cipher.getInstance("AES");

        cipher.init(Cipher.DECRYPT_MODE, key);

        return cipher.doFinal(data, 0, length);
    }
}
