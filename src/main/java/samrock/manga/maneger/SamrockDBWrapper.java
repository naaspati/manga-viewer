package samrock.manga.maneger;

import java.sql.SQLException;

import sam.manga.newsamrock.SamrockDB;

public class SamrockDBWrapper extends SamrockDB {

	public SamrockDBWrapper() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		super();
	}
	
	@Override
	protected void finalize() throws Throwable {
		close();
	}

}
