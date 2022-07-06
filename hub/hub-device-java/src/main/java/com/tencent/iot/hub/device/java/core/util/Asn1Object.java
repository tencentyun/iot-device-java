package com.tencent.iot.hub.device.java.core.util;


import java.io.IOException;
import java.math.BigInteger;

/**
 * Asn1 编码类
 */
public class Asn1Object {

    protected final int type;
    protected final int length;
    protected final byte[] value;
    protected final int tag;

    /**
     * 构造函数
     *
     * @param tag 标记
     * @param length 长度
     * @param value 编码源
     */
    public Asn1Object(int tag, int length, byte[] value) {
        this.tag = tag;
        this.type = tag & 0x1F;
        this.length = length;
        this.value = value;
    }

    /**
     * 获取类型
     * @return 类型
     */
    public int getType() {
        return type;
    }

    /**
     * 获取长度
     * @return 长度
     */
    public int getLength() {
        return length;
    }

    /**
     * 获取编码源
     * @return 编码源
     */
    public byte[] getValue() {
        return value;
    }

    /**
     * 是否已构造
     *
     * @return true：已构造；false：未构造
     */
    public boolean isConstructed() {
        return (tag & DerParser.CONSTRUCTED) == DerParser.CONSTRUCTED;
    }

    /**
     * 获取解析器
     *
     * @return 解析器 {@link DerParser}
     * @throws IOException
     */
    public DerParser getParser() throws IOException {
        if (!isConstructed())
            throw new IOException("Invalid DER: can't parse primitive entity"); //$NON-NLS-1$

        return new DerParser(value);
    }

    /**
     * 获取 Integer 类型的值
     *
     * @return {@link BigInteger}
     * @throws IOException
     */
    public BigInteger getInteger() throws IOException {
        if (type != DerParser.INTEGER)
            throw new IOException("Invalid DER: object is not integer"); //$NON-NLS-1$

        return new BigInteger(value);
    }

    /**
     * 获取 String 类型的值
     *
     * @return String 类型的值
     * @throws IOException
     */
    public String getString() throws IOException {

        String encoding;

        switch (type) {

            // Not all are Latin-1 but it's the closest thing
            case DerParser.NUMERIC_STRING:
            case DerParser.PRINTABLE_STRING:
            case DerParser.VIDEOTEX_STRING:
            case DerParser.IA5_STRING:
            case DerParser.GRAPHIC_STRING:
            case DerParser.ISO646_STRING:
            case DerParser.GENERAL_STRING:
                encoding = "ISO-8859-1"; //$NON-NLS-1$
                break;

            case DerParser.BMP_STRING:
                encoding = "UTF-16BE"; //$NON-NLS-1$
                break;

            case DerParser.UTF8_STRING:
                encoding = "UTF-8"; //$NON-NLS-1$
                break;

            case DerParser.UNIVERSAL_STRING:
                throw new IOException("Invalid DER: can't handle UCS-4 string"); //$NON-NLS-1$

            default:
                throw new IOException("Invalid DER: object is not a string"); //$NON-NLS-1$
        }

        return new String(value, encoding);
    }
}

