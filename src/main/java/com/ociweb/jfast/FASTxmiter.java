package com.ociweb.jfast;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;


public interface FASTxmiter {
	
	//all the supported basic types
	void write(int id, long value);
	void write(int id, int value);
	void write(int id, int exponent, long mantissa);
	
	//multiple ways to send bytes
	void write(int id, byte[] value, int offset, int length);
	void write(int id, ByteBuffer buffer);
	
	//multiple ways to send chars
	void write(int id, CharSequence value); //String is a CharSequence
	void write(int id, CharBuffer buffer);
	void write(int id, char[] value, int offset, int length);
		
	void write(int id);//null
	
	void openGroup(int id);
	void openGroup(int id, int repeat);
	void closeGroup();
	void flush();
	
}
