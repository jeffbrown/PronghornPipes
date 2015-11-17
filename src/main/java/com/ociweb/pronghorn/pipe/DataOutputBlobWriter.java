package com.ociweb.pronghorn.pipe;

import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class DataOutputBlobWriter<S extends MessageSchema> extends OutputStream implements DataOutput {

    private final Pipe<S> p;
    private final byte[] byteBuffer;
    private final int byteMask;
    
    private ObjectOutputStream oos;
    
    private int startPosition;
    private int activePosition;
    
    public DataOutputBlobWriter(Pipe<S> p) {
        this.p = p;
        this.byteBuffer = Pipe.byteBuffer(p);
        this.byteMask = Pipe.blobMask(p);        
    }
    
    public void openField() {
        p.openBlobFieldWrite();
        startPosition = activePosition = Pipe.bytesWorkingHeadPosition(p);
    }
    
    public int closeField(int targetFieldLoc) {
        //this method will also validate the length was in bound and throw unsupported operation if the pipe was not large enough
        //instead of fail fast as soon as one field goes over we wait to the end and only check once.
        int len = length();
        PipeWriter.writeSpecialBytesPosAndLen(p, targetFieldLoc, len, startPosition);
        p.closeBlobFieldWrite();
        return len;
    }
    
    public int length() {
        if (activePosition>=startPosition) {
            return activePosition-startPosition;            
        } else {        
            return (activePosition-Integer.MIN_VALUE)+(1+Integer.MAX_VALUE-startPosition);
        }
    }
    
    public byte[] toByteArray() {
        byte[] result = new byte[length()];        
        Pipe.copyBytesFromToRing(byteBuffer, startPosition, byteMask, result, 0, Integer.MAX_VALUE, result.length);
        return result;
    }
    
    public void writeObject(Object object) throws IOException {
            if (null==oos) {
                oos = new ObjectOutputStream(this);
            }            
            oos.writeObject(object); //TODO:: this needs testing
            oos.flush();
    }
    
    @Override
    public void write(int b) throws IOException {
        byteBuffer[byteMask & activePosition++] = (byte)b;
    }

    @Override
    public void write(byte[] b) throws IOException { 
        Pipe.copyBytesFromToRing(b, 0, Integer.MAX_VALUE, byteBuffer, activePosition, byteMask, b.length);  
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        Pipe.copyBytesFromToRing(b, off, Integer.MAX_VALUE, byteBuffer, activePosition, byteMask, len); 
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        byteBuffer[byteMask & activePosition++] = (byte) (v ? 1 : 0);
    }

    @Override
    public void writeByte(int v) throws IOException {
        byteBuffer[byteMask & activePosition++] = (byte)v;
    }

    @Override
    public void writeShort(int v) throws IOException {
        activePosition = write16(byteBuffer, byteMask, activePosition, v); 
    }

    @Override
    public void writeChar(int v) throws IOException {
        activePosition = write16(byteBuffer, byteMask, activePosition, v); 
    }

    @Override
    public void writeInt(int v) throws IOException {
        activePosition = write32(byteBuffer, byteMask, activePosition, v); 
    }

    @Override
    public void writeLong(long v) throws IOException {
        activePosition = write64(byteBuffer, byteMask, activePosition, v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        activePosition = write32(byteBuffer, byteMask, activePosition, Float.floatToIntBits(v));
    }

    @Override
    public void writeDouble(double v) throws IOException {
        activePosition = write64(byteBuffer, byteMask, activePosition, Double.doubleToLongBits(v));
    }

    @Override
    public void writeBytes(String s) throws IOException {
        byte[] localBuf = byteBuffer;
        int mask = byteMask;
        int pos = activePosition;
        int len = s.length();
        for (int i = 0; i < len; i ++) {
            localBuf[mask & pos++] = (byte) s.charAt(i);
        }
        activePosition = pos;
    }

    @Override
    public void writeChars(String s) throws IOException {
        byte[] localBuf = byteBuffer;
        int mask = byteMask;
        int pos = activePosition;
        int len = s.length();
        for (int i = 0; i < len; i ++) {
            pos = write16(localBuf, mask, pos, (int) s.charAt(i));
        }
        activePosition = pos;
        
    }


    @Override
    public void writeUTF(String s) throws IOException {
        activePosition = encodeAsUTF8(s, s.length(), byteMask, byteBuffer, activePosition);
    }

    private int encodeAsUTF8(CharSequence s, int len, int mask, byte[] localBuf, int pos) {
        int c = 0;
        while (c < len) {
            pos = Pipe.encodeSingleChar((int) s.charAt(c++), localBuf, mask, pos);
        }
        return pos;
    }
    
    ///////////
    //end of DataOutput methods
    ////////// 

    private static int write16(byte[] buf, int mask, int pos, int v) {
        buf[mask & pos++] = (byte)(v >>> 8);
        buf[mask & pos++] = (byte) v;
        return pos;
    }    
    
    private static int write32(byte[] buf, int mask, int pos, int v) {
        buf[mask & pos++] = (byte)(v >>> 24);
        buf[mask & pos++] = (byte)(v >>> 16);
        buf[mask & pos++] = (byte)(v >>> 8);
        buf[mask & pos++] = (byte) v;
        return pos;
    }
    
    private static int write64(byte[] buf, int mask, int pos, long v) {
        buf[mask & pos++] = (byte)(v >>> 56);
        buf[mask & pos++] = (byte)(v >>> 48);
        buf[mask & pos++] = (byte)(v >>> 40);
        buf[mask & pos++] = (byte)(v >>> 32);
        buf[mask & pos++] = (byte)(v >>> 24);
        buf[mask & pos++] = (byte)(v >>> 16);
        buf[mask & pos++] = (byte)(v >>> 8);
        buf[mask & pos++] = (byte) v;
        return pos;
    }
    
    public void writeUTF(CharSequence s) throws IOException {
        activePosition = encodeAsUTF8(s, s.length(), byteMask, byteBuffer, activePosition);
    }    
    
    public void writeASCII(CharSequence s) {
        byte[] localBuf = byteBuffer;
        int mask = byteMask;
        int pos = activePosition;
        int len = s.length();        
        for (int i = 0; i < len; i ++) {
            localBuf[mask & pos++] = (byte)s.charAt(i);
        }
        activePosition = pos;
    }
    
    public void writeByteArray(byte[] bytes) throws IOException {
        activePosition = writeByteArray(bytes, bytes.length, byteBuffer, byteMask, activePosition);
    }

    private int writeByteArray(byte[] bytes, int len, byte[] bufLocal, int mask, int pos) {
        pos = write32(bufLocal, mask, pos, len);
        for(int i=0;i<len;i++) {
            bufLocal[mask & pos++] = (byte) bytes[i];
        }
        return pos;
    }

    public void writeCharArray(char[] chars) throws IOException {
        activePosition = writeCharArray(chars, chars.length, byteBuffer, byteMask, activePosition);
    }

    private int writeCharArray(char[] chars, int len, byte[] bufLocal, int mask, int pos) {
        pos = write32(bufLocal, mask, pos, len);
        for(int i=0;i<len;i++) {
            pos = write16(bufLocal, mask, pos, (int) chars[i]);
        }
        return pos;
    }

    public void writeIntArray(int[] ints) throws IOException {
        activePosition = writeIntArray(ints, ints.length, byteBuffer, byteMask, activePosition);
    }

    private int writeIntArray(int[] ints, int len, byte[] bufLocal, int mask, int pos) {
        pos = write32(bufLocal, mask, pos, len);
        for(int i=0;i<len;i++) {
            pos = write32(bufLocal, mask, pos, ints[i]);
        }
        return pos;
    }

    public void writeLongArray(long[] longs) throws IOException {
        activePosition = writeLongArray(longs, longs.length, byteBuffer, byteMask, activePosition);
    }

    private int writeLongArray(long[] longs, int len, byte[] bufLocal, int mask, int pos) {
        pos = write32(bufLocal, mask, pos, len);
        for(int i=0;i<len;i++) {
            pos = write64(bufLocal, mask, pos, longs[i]);
        }
        return pos;
    }

    public void writeDoubleArray(double[] doubles) throws IOException {
        activePosition = writeDoubleArray(doubles, doubles.length, byteBuffer, byteMask, activePosition);
    }

    private int writeDoubleArray(double[] doubles, int len, byte[] bufLocal, int mask, int pos) {
        pos = write32(bufLocal, mask, pos, len);
        for(int i=0;i<len;i++) {
            pos = write64(bufLocal, mask, pos, Double.doubleToLongBits(doubles[i]));
        }
        return pos;
    }

    public void writeFloatArray(float[] floats) throws IOException {
        activePosition = writeFloatArray(floats, floats.length, byteBuffer, byteMask, activePosition);
    }

    private int writeFloatArray(float[] floats, int len, byte[] bufLocal, int mask, int pos) {
        pos = write32(bufLocal, mask, pos, len);
        for(int i=0;i<len;i++) {
            pos = write32(bufLocal, mask, pos, Float.floatToIntBits(floats[i]));
        }
        return pos;
    }

    public void writeShortArray(short[] shorts) throws IOException {
        activePosition = writeShortArray(shorts, shorts.length, byteBuffer, byteMask, activePosition);
    }

    private int writeShortArray(short[] shorts, int len, byte[] bufLocal, int mask, int pos) {
        pos = write32(bufLocal, mask, pos, len);
        for(int i=0;i<len;i++) {
            pos = write16(bufLocal, mask, pos, shorts[i]);
        }
        return pos;
    }

    public void writeBooleanArray(boolean[] booleans) throws IOException {
        activePosition = writeBooleanArray(booleans, booleans.length, byteBuffer, byteMask, activePosition);
    }

    private int writeBooleanArray(boolean[] booleans, int len, byte[] bufLocal, int mask, int pos) {
        pos = write32(bufLocal, mask, pos, len);
        for(int i=0;i<len;i++) {
            bufLocal[mask & pos++] = (byte) (booleans[i] ? 1 : 0);
        }
        return pos;
    }

    public void writeUTFArray(String[] utfs) throws IOException {
        activePosition = writeUTFArray(utfs, utfs.length, byteBuffer, byteMask, activePosition);
    }

    private int writeUTFArray(String[] utfs, int len, byte[] bufLocal, int mask, int pos) {
        pos = write32(bufLocal, mask, pos, len);
        for(int i=0;i<len;i++) {
            pos = encodeAsUTF8(utfs[i], utfs[i].length(), mask, bufLocal, pos);
        }
        return pos;
    }

    ///////////////////////////////////////////////////////////////////////////////////
    //Support for packed values
    //////////////////////////////////////////////////////////////////////////////////
    //Write signed using variable length encoding as defined in FAST 1.1 specification
    //////////////////////////////////////////////////////////////////////////////////
    
    public final void writePackedLong(long value) {
        if (value >=0) {
            activePosition = writeLongSignedPos(value, byteBuffer, byteMask, activePosition);
        } else {
            activePosition = writeLongSignedNeg(value, byteBuffer, byteMask, activePosition);
        }
    }
    
    public final void writePackedInt(int value) {
        if (value >=0) {
            activePosition = writeIntSignedPos(value, byteBuffer, byteMask, activePosition);
        } else {
            activePosition = writeIntSignedNeg(value, byteBuffer, byteMask, activePosition);
        }
    }
    
    public final void writePackedShort(short value) {
        if (value >=0) {
            activePosition = writeIntSignedPos(value, byteBuffer, byteMask, activePosition);
        } else {
            activePosition = writeIntSignedNeg(value, byteBuffer, byteMask, activePosition);
        }
    }
    
    private static final int writeLongSignedNeg(long value, byte[] buf, int mask, int pos) {
        // using absolute value avoids tricky word length issues
        long absv = -value;

        if (absv <= 0x0000000000000040l) {
        } else {
            if (absv <= 0x0000000000002000l) {
            } else {
                if (absv <= 0x0000000000100000l) {
                } else {
                    if (absv <= 0x0000000008000000l) {
                    } else {
                        if (absv <= 0x0000000400000000l) {
                        } else {
                            if (absv <= 0x0000020000000000l) {
                            } else {
                                if (absv <= 0x0001000000000000l) {
                                } else {
                                    if (absv <= 0x0080000000000000l) { 
                                    } else {
                                        long lastBit = value>>>63;
                                        if (0 != lastBit) {
                                            buf[mask & pos++] =  (byte) lastBit;
                                        } 
                                       buf[mask & pos++] =  (byte) (( ((int)(value >>> 56)) & 0x7F));
                                    }
                                    buf[mask & pos++] = (byte) (( ((int)(value >>> 49)) & 0x7F));
                                }
                                buf[mask & pos++] = (byte) (( ((int)(value >>> 42)) & 0x7F));
                            }
                            buf[mask & pos++] = (byte) (( ((int)(value >>> 35)) & 0x7F));
                        }
                        buf[mask & pos++] = (byte) (( ((int)(value >>> 28)) & 0x7F));
                    }
                    buf[mask & pos++] = (byte) (( ((int)(value >>> 21)) & 0x7F));
                }
                buf[mask & pos++] = (byte) (( ((int)(value >>> 14)) & 0x7F));
            }
            buf[mask & pos++] = (byte) (( ((int)(value >>> 7)) & 0x7F));
        }
        buf[mask & pos++] = (byte) (( ((int)(value & 0x7F)) | 0x80));
        return pos;
    }

    
    private static final int writeIntSignedNeg(int value, byte[] buf, int mask, int pos) {
        // using absolute value avoids tricky word length issues
        long absv = -value;

        if (absv <= 0x0000000000000040) {
        } else {
            if (absv <= 0x0000000000002000) {
            } else {
                if (absv <= 0x0000000000100000) {
                } else {
                    if (absv <= 0x0000000008000000) {
                    } else {
                        buf[mask & pos++] = (byte) (((value >>> 28) & 0x7F));
                    }
                    buf[mask & pos++] = (byte) (((value >>> 21) & 0x7F));
                }
                buf[mask & pos++] = (byte) (((value >>> 14) & 0x7F));
            }
            buf[mask & pos++] = (byte) (((value >>> 7) & 0x7F));
        }
        buf[mask & pos++] = (byte) (((value & 0x7F) | 0x80));
        return pos;
    }
        
    
    

    private static final int writeLongSignedPos(long value, byte[] buf, int mask, int pos) {

        if (value < 0x0000000000000040l) {
        } else {
            if (value < 0x0000000000002000l) {
            } else {
                if (value < 0x0000000000100000l) {
                } else {
                    if (value < 0x0000000008000000l) {
                    } else {
                        if (value < 0x0000000400000000l) {
                        } else {
                            if (value < 0x0000020000000000l) {
                            } else {
                                if (value < 0x0001000000000000l) {
                                } else {
                                    if (value < 0x0080000000000000l) {
                                    } else {
                                        if (value < 0x4000000000000000l) {
                                        } else {
                                            buf[mask & pos++] = (byte) (( ((int)(value >>> 63)) & 0x7F));
                                        }
                                        buf[mask & pos++] = (byte) (( ((int)(value >>> 56)) & 0x7F));
                                    }
                                    buf[mask & pos++] = (byte) (( ((int)(value >>> 49)) & 0x7F));
                                }
                                buf[mask & pos++] = (byte) (( ((int)(value >>> 42)) & 0x7F));
                            }
                            buf[mask & pos++] =(byte) (( ((int)(value >>> 35)) & 0x7F));
                        }
                        buf[mask & pos++] = (byte) (( ((int)(value >>> 28)) & 0x7F));
                    }
                    buf[mask & pos++] = (byte) (( ((int)(value >>> 21)) & 0x7F));
                }
                buf[mask & pos++] = (byte) (( ((int)(value >>> 14)) & 0x7F));
            }
            buf[mask & pos++] = (byte) (( ((int)(value >>> 7)) & 0x7F));
        }
        buf[mask & pos++] = (byte) (( ((int)(value & 0x7F)) | 0x80));
        return pos;
    }
    

    private static final int writeIntSignedPos(int value, byte[] buf, int mask, int pos) {

        if (value < 0x0000000000000040) {
        } else {
            if (value < 0x0000000000002000) {
            } else {
                if (value < 0x0000000000100000) {
                } else {
                    if (value < 0x0000000008000000) {
                    } else {                        
                        buf[mask & pos++] = (byte) (((value >>> 28) & 0x7F));
                    }
                    buf[mask & pos++] = (byte) (((value >>> 21) & 0x7F));
                }
                buf[mask & pos++] = (byte) (((value >>> 14) & 0x7F));
            }
            buf[mask & pos++] = (byte) (((value >>> 7) & 0x7F));
        }
        buf[mask & pos++] = (byte) (((value & 0x7F) | 0x80));
        return pos;
    }
    
    
    
}
