package com.ociweb.jfast.field.util;

import com.ociweb.jfast.FASTxmiter;
import com.ociweb.jfast.FASTProvide;
import com.ociweb.jfast.primitive.PrimitiveReader;
import com.ociweb.jfast.primitive.PrimitiveWriter;

public final class FieldStopper extends Field {

	//TODO: stopper must call end methods in visitor and provider
	
	public void reader(PrimitiveReader reader, FASTxmiter visitor) {
	}

	public void writer(PrimitiveWriter writer, FASTProvide provider) {
	}
	
	public void reset() {
	}
}
