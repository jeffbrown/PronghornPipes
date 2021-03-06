package com.ociweb.pronghorn.pipe.stream;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;

import com.ociweb.pronghorn.util.Appendables;
import com.ociweb.pronghorn.util.TrieParserReader;
import com.ociweb.pronghorn.util.parse.JSONParser;
import com.ociweb.pronghorn.util.parse.JSONVisitor;
import com.ociweb.pronghorn.util.parse.JSONVisitorNull;

public class StreamingReadVisitorToJSON<A extends Appendable> implements StreamingReadVisitor {

	private final StringBuilder tempStringBuilder;
	private ByteBuffer tempByteBuffer;
	
	private A out;
	private int depth = 0;
	private int step = 2;
	private final boolean showBytesAsUTF;
	
	public StreamingReadVisitorToJSON(A out) {
		this(out,4096, 256, false);
	}
	
	public StreamingReadVisitorToJSON(A out, boolean showBytesAsUTF) {
		this(out,4096, 256, showBytesAsUTF);
	}
	
	public StreamingReadVisitorToJSON(A out, int maxBytesSize, int maxStringSize) {
		this(out,maxBytesSize, maxStringSize, false);
    }
	
	public StreamingReadVisitorToJSON(A out, int maxBytesSize, int maxStringSize, boolean showBytesAsUTF) {
	    this.out = out;
	    this.tempByteBuffer = ByteBuffer.allocate(maxBytesSize);
	    this.tempStringBuilder =  new StringBuilder(maxStringSize);
	    this.showBytesAsUTF = showBytesAsUTF;
	}
	
	
	@Override
	public boolean paused() {
		return false; //not used in this case because we block on out
	}

	private void writeTab() {
		try {
			int j = depth;
			while (--j>=0) {
				out.append(' ');
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}	
	}
	
	@Override
	public void visitTemplateOpen(String name, long id) {
		//no tab needed here
		try {
			out.append("{\""+name+"\":");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}			
		depth = step;
	}
	
	@Override
	public void visitTemplateClose(String name, long id) {
		depth -= step;
		writeTab();
		try {
			out.append("}");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}

	@Override
	public void visitFragmentOpen(String name, long id, int cursor) {
		writeTab();
		try{
			out.append("{\""+name+"\":");	
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
		depth += step;
	}

	@Override
	public void visitFragmentClose(String name, long id) {
		depth -= step;
		writeTab();
		try {
			out.append("}");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}	
	}

	@Override
	public void visitSequenceOpen(String name, long id, int length) {
		writeTab();
		try{
			out.append("[");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
		depth += step;
	}

	@Override
	public void visitSequenceClose(String name, long id) {
		depth -= step;
		writeTab();
		try {
			out.append("]");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}

	@Override
	public void visitSignedInteger(String name, long id, int value) {
		writeTab();
		try {
			out.append("{\""+name+"\":"+Integer.valueOf(value)+"}");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}

	@Override
	public void visitUnsignedInteger(String name, long id, long value) {
		writeTab();
		try {
			out.append("{\""+name+"\":"+Long.valueOf(value)+"}");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}

	@Override
	public void visitSignedLong(String name, long id, long value) {
		writeTab();
		try {
			out.append("{\"");
			out.append(name);
			out.append("\":");
			out.append(Long.valueOf(value)+"}");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}

	@Override
	public void visitUnsignedLong(String name, long id, long value) {
		writeTab();
		try {
			out.append("{\"");
			out.append(name);
			out.append("\":");
			out.append(Long.valueOf(value)+"}"); //TODO: this is not strictly right and can be negative!!
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}

	@Override
	public void visitDecimal(String name, long id, int exp, long mant) {
		writeTab();
		try {
			out.append("{\""+name+"\":["+Integer.valueOf(exp)+","+Long.valueOf(mant)+"]}");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}

	@Override
	public Appendable targetASCII(String name, long id) {
		tempStringBuilder.setLength(0);
		return tempStringBuilder;
	}

	@Override
	public void visitASCII(String name, long id, CharSequence value) {
		writeTab();
		try {
			out.append("{\""+name+"\":\""+value+"\"}");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}	
	}

	@Override
	public Appendable targetUTF8(String name, long id) {
		tempStringBuilder.setLength(0);
		return tempStringBuilder;
	}

	@Override
	public void visitUTF8(String name, long id, CharSequence value) {
		writeTab();
		try {
			out.append("{\"");
	        out.append(name);
	        out.append("\":\"");
	        out.append(value);
			out.append("\"}");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}	
	}

	@Override
	public ByteBuffer targetBytes(String name, long id, int length) {
		tempByteBuffer.clear();
		if (tempByteBuffer.capacity()<length) {
			tempByteBuffer = ByteBuffer.allocate(length*2);
		}
		return tempByteBuffer;
	}

	@Override
	public void visitBytes(String name, long id, ByteBuffer value) {
		value.flip();

		writeTab();
        try {
			out.append("{\"");
	        out.append(name);
	        out.append("\":\"");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}	
        
        if (showBytesAsUTF) {   
        	
        	Appendables.appendUTF8(out, value.array(), value.position(), value.remaining(), Integer.MAX_VALUE);     
        	
        } else {
   
	        while (value.hasRemaining()) {
	 			Appendables.appendFixedHexDigits(out, 0xFF&value.get(), 8);
	
	            if (value.hasRemaining()) {
	            	try {
	            		out.append(",");
	        		} catch (IOException e) {
	        			throw new RuntimeException(e);
	        		}	
	            }
	
	        }
        }
        try {
        	out.append("\"}");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}	
        
	}

	@Override
	public void startup() {
	}

	@Override
	public void shutdown() {
	}

}
