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

package eu.jacquet80.rds.app.oda;

import java.util.HashMap;
import java.util.Map;

import eu.jacquet80.rds.app.Application;

public abstract class ODA extends Application {
	private static Map<Integer, Class<? extends ODA>> odas = new HashMap<Integer, Class<? extends ODA>>();
	
	public abstract int getAID();
	
	private static void register(int aid, Class<? extends ODA> oda) {
		odas.put(aid, oda);
	}
	
	public static ODA forAID(int aid) {
		try {
			Class<? extends ODA> theClass = odas.get(aid);
			if(theClass != null) return theClass.newInstance();
			else return null;
		} catch (InstantiationException e) {
			return null;
		} catch (IllegalAccessException e) {
			return null;
		}
	}
	
	static {
		register(RTPlus.AID, RTPlus.class);
		register(AlertC.AID, AlertC.class);
		register(AlertCwithAlertPlus.AID, AlertC.class);
	}
}
