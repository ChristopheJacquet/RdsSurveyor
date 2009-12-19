package eu.jacquet80.rds.core;

import java.io.IOException;

import eu.jacquet80.rds.input.RDSReader;
import eu.jacquet80.rds.log.Log;

public interface RDSDecoder {
	public void processStream(RDSReader reader, Log log) throws IOException;
	public TunedStation getTunedStation();
}
