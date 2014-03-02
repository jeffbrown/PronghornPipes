//Copyright 2013, Nathan Tippy
//See LICENSE file for BSD license details.
//Send support requests to http://www.ociweb.com/contact
package com.ociweb.jfast.field;

import com.ociweb.jfast.loader.DictionaryFactory;
import com.ociweb.jfast.primitive.PrimitiveReader;

public class FieldReaderDecimal {
	
	private PrimitiveReader reader;
	private final FieldReaderInteger exponent;
	private final FieldReaderLong mantissa;
		
	
	public FieldReaderDecimal(PrimitiveReader reader, int[] decimalExponentDictionary, long[] decimalMantissaDictionary) {
		this.reader = reader;
		this.exponent = new FieldReaderInteger(reader, decimalExponentDictionary);
		this.mantissa = new FieldReaderLong(reader, decimalMantissaDictionary);
	}

	//TODO: Optional absent null is not implemented yet for Decimal type.
	
	public void reset(DictionaryFactory df) {
		df.reset(exponent.lastValue,mantissa.lastValue);
	}	
	public void copyExponent(int sourceToken, int targetToken) {
		exponent.copy(sourceToken, targetToken);
	}
	public void copyMantissa(int sourceToken, int targetToken) {
		mantissa.copy(sourceToken, targetToken);
	}
	
	public int readDecimalExponentOptional(int token, int valueOfOptional) {
		
		//oppExp
				if (0==(token&(1<<(TokenBuilder.SHIFT_OPER+TokenBuilder.SHIFT_OPER_DECIMAL_EX)))) {
					//none, constant, delta
					if (0==(token&(2<<(TokenBuilder.SHIFT_OPER+TokenBuilder.SHIFT_OPER_DECIMAL_EX)))) {
						//none, delta
						if (0==(token&(4<<(TokenBuilder.SHIFT_OPER+TokenBuilder.SHIFT_OPER_DECIMAL_EX)))) {
							//none
							return exponent.readIntegerSignedOptional(token,valueOfOptional);
						} else {
							//delta
							return exponent.readIntegerSignedDeltaOptional(token, valueOfOptional);
						}	
					} else {
						//constant
						return exponent.readIntegerSignedConstantOptional(token, valueOfOptional);
					}
					
				} else {
					//copy, default, increment
					if (0==(token&(2<<(TokenBuilder.SHIFT_OPER+TokenBuilder.SHIFT_OPER_DECIMAL_EX)))) {
						//copy, increment
						if (0==(token&(4<<(TokenBuilder.SHIFT_OPER+TokenBuilder.SHIFT_OPER_DECIMAL_EX)))) {
							//copy
							return exponent.readIntegerSignedCopyOptional(token,valueOfOptional);
						} else {
							//increment
							return exponent.readIntegerSignedIncrementOptional(token, valueOfOptional);
						}	
					} else {
						// default
						return exponent.readIntegerSignedDefaultOptional(token, valueOfOptional);
					}		
				}
				
	}

	public int readDecimalExponent(int token) {
		
		//oppExp
		if (0==(token&(1<<(TokenBuilder.SHIFT_OPER+TokenBuilder.SHIFT_OPER_DECIMAL_EX)))) {
			//none, constant, delta
			if (0==(token&(2<<(TokenBuilder.SHIFT_OPER+TokenBuilder.SHIFT_OPER_DECIMAL_EX)))) {
				//none, delta
				if (0==(token&(4<<(TokenBuilder.SHIFT_OPER+TokenBuilder.SHIFT_OPER_DECIMAL_EX)))) {
					//none
					return exponent.readIntegerSigned(token);
				} else {
					//delta
					return exponent.readIntegerSignedDelta(token);
				}	
			} else {
				//constant
				return exponent.readIntegerSignedConstant(token);
			}
			
		} else {
			//copy, default, increment
			if (0==(token&(2<<(TokenBuilder.SHIFT_OPER+TokenBuilder.SHIFT_OPER_DECIMAL_EX)))) {
				//copy, increment
				if (0==(token&(4<<(TokenBuilder.SHIFT_OPER+TokenBuilder.SHIFT_OPER_DECIMAL_EX)))) {
					//copy
					return exponent.readIntegerSignedCopy(token);
				} else {
					//increment
					return exponent.readIntegerSignedIncrement(token);
				}	
			} else {
				// default
				return exponent.readIntegerSignedDefault(token);
			}		
		}
		
	}

	public long readDecimalMantissaOptional(int token, long valueOfOptional) {
		//oppMaint
				if (0==(token&(1<<TokenBuilder.SHIFT_OPER))) {
					//none, constant, delta
					if (0==(token&(2<<TokenBuilder.SHIFT_OPER))) {
						//none, delta
						if (0==(token&(4<<TokenBuilder.SHIFT_OPER))) {
							//none
							return mantissa.readLongSignedOptional(token,valueOfOptional);
						} else {
							//delta
							return mantissa.readLongSignedDeltaOptional(token, valueOfOptional);
						}	
					} else {
						//constant
						return mantissa.readLongSignedConstantOptional(token, valueOfOptional);
					}
					
				} else {
					//copy, default, increment
					if (0==(token&(2<<TokenBuilder.SHIFT_OPER))) {
						//copy, increment
						if (0==(token&(4<<TokenBuilder.SHIFT_OPER))) {
							//copy
							return mantissa.readLongSignedCopyOptional(token,valueOfOptional);
						} else {
							//increment
							return mantissa.readLongSignedIncrementOptional(token, valueOfOptional);
						}	
					} else {
						// default
						return mantissa.readLongSignedDefaultOptional(token, valueOfOptional);
					}		
				}
		
	}

	public long readDecimalMantissa(int token) {
		if (0==(token&(1<<TokenBuilder.SHIFT_OPER))) {
			//none, constant, delta
			if (0==(token&(2<<TokenBuilder.SHIFT_OPER))) {
				//none, delta
				if (0==(token&(4<<TokenBuilder.SHIFT_OPER))) {
					//none
					return mantissa.readLongSigned(token);
				} else {
					//delta
					return mantissa.readLongSignedDelta(token);
				}	
			} else {
				//constant
				return mantissa.readLongSignedConstant(token);
			}
			
		} else {
			//copy, default, increment
			if (0==(token&(2<<TokenBuilder.SHIFT_OPER))) {
				//copy, increment
				if (0==(token&(4<<TokenBuilder.SHIFT_OPER))) {
					//copy
					return mantissa.readLongSignedCopy(token);
				} else {
					//increment
					return mantissa.readLongSignedIncrement(token);
				}	
			} else {
				// default
				return mantissa.readLongSignedDefault(token);
			}		
		}
		
		
	}



}
