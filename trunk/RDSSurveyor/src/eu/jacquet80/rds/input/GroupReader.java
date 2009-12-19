package eu.jacquet80.rds.input;

import java.io.IOException;

public interface GroupReader extends RDSReader {
	public int[] getGroup() throws IOException;
}
