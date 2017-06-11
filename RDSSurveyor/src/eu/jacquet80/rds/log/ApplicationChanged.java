/*
 RDS Surveyor -- RDS decoder, analyzer and monitor tool and library.
 For more information see
   http://www.jacquet80.eu/
   http://rds-surveyor.sourceforge.net/
 
 Copyright (c) 2009, 2010 Christophe Jacquet

 This file is part of RDS Surveyor.

 RDS Surveyor is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 RDS Surveyor is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser Public License for more details.

 You should have received a copy of the GNU Lesser Public License
 along with RDS Surveyor.  If not, see <http://www.gnu.org/licenses/>.

*/

package eu.jacquet80.rds.log;

import eu.jacquet80.rds.app.Application;

/**
 * Reported when a new app has been encountered for the first time in the RDS data stream of the
 * tuned station and a handler for it has been registered.
 */
public class ApplicationChanged extends LogMessage {
	private final Application oldApp, newApp;

	public ApplicationChanged(RDSTime time, Application oldApp, Application newApp) {
		super(time);
		this.oldApp = oldApp;
		this.newApp = newApp;
	}

	@Override
	public void accept(LogMessageVisitor visitor) {
		visitor.visit(this);
	}

	/**
	 * @brief Reserved, currently returns null.
	 */
	public Application getOldApplication() {
		return oldApp;
	}
	
	/**
	 * @brief Returns an instance to the newly discovered application.
	 */
	public Application getNewApplication() {
		return newApp;
	}
}
