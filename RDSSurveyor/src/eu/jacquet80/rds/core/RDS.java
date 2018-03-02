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

package eu.jacquet80.rds.core;

import java.util.Locale;


public class RDS {
	private final static int matH[] = {
		0x31B, 0x38F, 0x2A7, 0x0F7, 0x1EE, 0x3DC, 0x201, 0x1BB, 0x376, 0x355,
		0x313, 0x39F, 0x287, 0x0B7, 0x16E, 0x2DC, 0x001, 0x002, 0x004, 0x008,
		0x010, 0x020, 0x040, 0x080, 0x100, 0x200
	};
	
	public final static int syndromes[][] = {{0x3D8, 0x3D8}, {0x3D4, 0x3D4}, {0x25C, 0x3CC}, {0x258, 0x258}};

	public final static int[] offsetWords = {
		0x0FC, 0x198, 0x168, 0x1B4
	};
	
	public final static String[][] languages = {
		{"Unknown", "??"},
		{"Albanian", "sq"},
		{"Breton", "br"},
		{"Catalan", "ca"},
		{"Croatian", "hr"},
		{"Welsh", "cy"},
		{"Czech", "cs"},
		{"Danish", "da"},
		{"German", "de"},
		{"English", "en"},
		{"Spanish", "es"},
		{"Esperanto", "eo"},
		{"Estonian", "et"},
		{"Basque", "eu"},
		{"Faroese", "fo"},
		{"French", "fr"},
		{"Frisian","fy"},
		{"Irish", "ga"},
		{"Gaelic", "gd"},
		{"Galician", "gl"},
		{"Icelandic", "is"},
		{"Italian", "it"},
		{"Lappish", "-lappish-"},
		{"Latin", "la"},
		{"Latvian", "lv"},
		{"Luxembourgian", "lb"},
		{"Lithuanian", "lt"},
		{"Hungarian", "hu"},
		{"Maltese", "mt"},
		{"Dutch", "nl"},
		{"Norwegian", "nn"},
		{"Occitan", "oc"},
		{"Polish", "pl"},
		{"Portuguese", "pt"},
		{"Romanian", "ro"},
		{"Romansh", "rm"},
		{"Serbian", "sr"},
		{"Slovak", "sk"},
		{"Slovene", "sl"},
		{"Finnish", "fi"},
		{"Swedish", "sv"},
		{"Turkish", "tr"},
		{"Flemish", "-flemish-"},
		{"Walloon", "wa"},
		{"<2C>", "2C"},
		{"<2D>", "2D"},
		{"<2E>", "2E"},
		{"<2F>", "2F"},
		{"<30>", "30"},
		{"<31>", "31"},
		{"<32>", "32"},
		{"<33>", "33"},
		{"<34>", "34"},
		{"<35>", "35"},
		{"<36>", "36"},
		{"<37>", "37"},
		{"<38>", "38"},
		{"<39>", "39"},
		{"Void", "-void-"},
		{"<41>", "41"},
		{"<42>", "42"},
		{"<43>", "43"},
		{"<44>", "44"},
		{"Zulu", "zu"}, 
		{"Vietnamese", "vi"},
		{"Uzbek", "uz"},
		{"Urdu", "ur"},
		{"Ukrainian", "uk"},
		{"Thai", "th"},
		{"Telugu", "te"},
		{"Tatar", "tt"},
		{"Tamil", "ta"},
		{"Tadzhik", "tg"},
		{"Swahili", "sw"},
		{"Sranan Tongo", "-sranan-tongo-"},
		{"Somali", "so"},
		{"Sinhalese", "si"},
		{"Shona", "sn"},
		{"Serbo-Croat", "sh"},
		{"Ruthenian", "-ruthenian-"},
		{"Russian", "ru"},
		{"Quechua", "qu"},
		{"Pushtu", "ps"},
		{"Punjabi", "pa"},
		{"Persian", "fa"},
		{"Papamiento", "-papamiento-"},
		{"Oriya", "or"},
		{"Nepali", "ne"},
		{"Ndebele", "nr"},
		{"Marathi", "mr"},
		{"Moldavian", "mo"},
		{"Malaysian", "ms"},
		{"Malagasay", "mg"},
		{"Macedonian", "mk"},
		{"Laotian", "lo"},
		{"Korean", "ko"},
		{"Khmer", "km"},
		{"Kazakh", "kk"},
		{"Kannada", "kn"},
		{"Japanese", "ja"},
		{"Indonesian", "id"},
		{"Hindi", "hi"},
		{"Hebrew", "he"},
		{"Hausa", "ha"},
		{"Gurani", "gn"},
		{"Gujurati", "gu"},
		{"Greek", "el"},
		{"Georgian", "ka"},
		{"Fulani", "ff"},
		{"Dari", "fa"},
		{"Churash", "cv"},
		{"Chinese", "zh"},
		{"Burmese", "my"},
		{"Bulgarian", "bg"},
		{"Bengali", "bn"},
		{"Belorussian", "be"},
		{"Bambora", "bm"},
		{"Azerbijani", "az"},
		{"Assamese", "as"},
		{"Armenian", "hy"},
		{"Arabic", "ar"},
		{"Amharic", "am"}
	};
	
	private final static String[] ecc_E0 = {"  ", "DE", "DZ", "AD", "IL", "IT", "BE", "RU", "PS", "AL", "AT", "HU", "MT", "DE", "  ", "EG"};
	private final static String[] ecc_E1 = {"  ", "GR", "CY", "SM", "CH", "JO", "FI", "LU", "BG", "DK", "GI", "IQ", "GB", "LY", "RO", "FR"};
	private final static String[] ecc_E2 = {"  ", "MA", "CZ", "PL", "VA", "SK", "SY", "TN", "  ", "LI", "IS", "MC", "LT", "RS/YU", "ES", "NO"};
	private final static String[] ecc_E3 = {"  ", "ME", "IE", "TR", "MK", "TJ", "  ", "  ", "NL", "LV", "LB", "AZ", "HR", "KZ", "SE", "BY"};
	private final static String[] ecc_E4 = {"  ", "MD", "EE", "KG", "  ", "  ", "UA", "KS", "PT", "SI", "AM", "UZ", "GE", "  ", "TM", "BA"};
	private final static String[] ecc_D0 = {"  ", "CM", "DZ/CF", "DJ", "MG", "ML", "AO", "GQ", "GA", "  ", "ZA", "BF", "CG", "TG", "BJ", "MW"};
	private final static String[] ecc_D1 = {"  ", "NA", "LR", "GH", "MR", "CV/ST", "  ", "SN", "GM", "BI", "??", "BW", "KM", "TZ", "ET", "NG"};
	private final static String[] ecc_D2 = {"  ", "SL", "ZW", "MZ", "UG", "SZ", "GN", "SO", "NE", "TD", "GW", "CD", "CI", "  ", "ZM", "ER"};
	private final static String[] ecc_D3 = {"  ", "  ", "  ", "EH", "??", "RW", "LS", "  ", "SC", "  ", "MU", "  ", "SD", "  ", "  ", "  "};
	private final static String[] ecc_A0 = {"  ", "US", "US", "US", "US", "US", "US", "US", "US", "US", "US", "US", "  ", "US", "US", "  "};
	private final static String[] ecc_A1 = {"  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "CA", "CA", "CA", "CA", "GL"};
	private final static String[] ecc_A2 = {"  ", "AI", "AG", "EC", "  ", "BB", "BZ", "KY", "CR", "CU", "AR", "BR", "BM", "AN", "GP", "BS"};
	private final static String[] ecc_A3 = {"  ", "BO", "CO", "JM", "MQ", "GF", "PY", "NI", "  ", "PA", "DM", "DO", "CL", "GD", "  ", "GY"};
	private final static String[] ecc_A4 = {"  ", "GT", "HN", "AW", "  ", "MS", "TT", "PE", "SR", "UY", "KN", "LC", "SV", "HT", "VE", "  "};
	private final static String[] ecc_A5 = {"  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "MX", "VC", "MX", "MX", "MX/VG"};
	private final static String[] ecc_A6 = {"  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "PM"};
	private final static String[] ecc_F0 = {"  ", "AU", "AU", "AU", "AU", "AU", "AU", "AU", "AU", "SA", "AF", "MM", "CN", "KP", "BH", "MY"};
	private final static String[] ecc_F1 = {"  ", "KI", "BT", "BD", "PK", "FJ", "OM", "NR", "IR", "NZ", "SB", "BN", "LK", "TW", "KR", "HK"};
	private final static String[] ecc_F2 = {"  ", "KW", "QA", "KH", "WS", "IN", "MO", "VN", "PH", "JP", "SG", "MV", "ID", "AE", "NP", "VU"};
	private final static String[] ecc_F3 = {"  ", "LA", "TH", "TO", "  ", "  ", "  ", "  ", "  ", "PG", "  ", "YE", "  ", "  ", "FM", "MN"};
	private final static String[] ecc_F4 = {"  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "  "};

	
	public final static String getISOCountryCode(int piCC, int ecc) {
		switch(ecc) {
		case 0: return "None";
		case 0xE0: return ecc_E0[piCC];
		case 0xE1: return ecc_E1[piCC];
		case 0xE2: return ecc_E2[piCC];
		case 0xE3: return ecc_E3[piCC];
		case 0xE4: return ecc_E4[piCC];
		case 0xD0: return ecc_D0[piCC];
		case 0xD1: return ecc_D1[piCC];
		case 0xD2: return ecc_D2[piCC];
		case 0xD3: return ecc_D3[piCC];
		case 0xA0: return ecc_A0[piCC];
		case 0xA1: return ecc_A1[piCC];
		case 0xA2: return ecc_A2[piCC];
		case 0xA3: return ecc_A3[piCC];
		case 0xA4: return ecc_A4[piCC];
		case 0xA5: return ecc_A5[piCC];
		case 0xA6: return ecc_A6[piCC];
		case 0xF0: return ecc_F0[piCC];
		case 0xF1: return ecc_F1[piCC];
		case 0xF2: return ecc_F2[piCC];
		case 0xF3: return ecc_F3[piCC];
		case 0xF4: return ecc_F4[piCC];
		default: return "Invalid";
		}
	}
	
	public final static String getCountryName(int piCC, int ecc) {
		String isoCC = getISOCountryCode(piCC, ecc);
		
		try {
			Locale locale = new Locale("en", isoCC);
			return locale.getDisplayCountry(Locale.ENGLISH);
		} catch(Exception e) {
			return isoCC;
		}
	}
	
	public final static int calcSyndrome(int bloc) {
		int synd = 0;
		for(int i=0; i<26; i++) {
			if((bloc & 1) != 0) synd ^= matH[i];
			bloc >>= 1;
		}
		return synd;
	}
	
	/*
	private static int poids(int codeword) {
		int poids = 0;
		for(int i=0; i<26; i++) {
			if((codeword & 1) != 0) poids++;
			codeword >>= 1;
		}
		return poids;
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		int[] errors = new int[1024];
		for(int cw=1; cw < (1<<26); cw++) {
			int synd = calcSyndrome(cw) >> 6;
			if(errors[synd] == 0) errors[synd] = cw;
			else {
				if(poids(cw) < poids(errors[synd])) errors[synd] = cw;
			}
			if(cw % 1000000 == 0) System.out.println(cw);
		}
		
		PrintWriter w = new PrintWriter(new File("/tmp/errors.txt"));
		for(int s=0; s<1024; s++) {
			w.printf("0x%07X, ", errors[s]);
			if(s%16 == 15) w.println();
		}
		w.flush();
		w.close();
	}
	*/
	
	private final static char[] charmap = new char[] {
		  '\u0000', '\u0020', '\u0020', '\u0020', '\u0020', '\u0020', '\u0020', '\u0020',
		  '\u0020', '\u0020', '\n',   	'\u000B', '\u0020', '\r',	  '\u0020', '\u0020',
		  '\u0020', '\u0020', '\u0020', '\u0020', '\u0020', '\u0020', '\u0020', '\u0020',
		  '\u0020', '\u0020', '\u0020', '\u0020', '\u0020', '\u0020', '\u0020', '\u001F',	
		  '\u0020',	'\u0021', '\u0022',	'\u0023', '\u00A4',	'\u0025', '\u0026',	'\'',     
		  '\u0028',	'\u0029', '\u002A',	'\u002B', '\u002C',	'\u002D', '\u002E',	'\u002F',	
		  '\u0030',	'\u0031', '\u0032',	'\u0033', '\u0034',	'\u0035', '\u0036',	'\u0037',	
		  '\u0038',	'\u0039', '\u003A',	'\u003B', '\u003C',	'\u003D', '\u003E',	'\u003F',	
		  '\u0040',	'\u0041', '\u0042',	'\u0043', '\u0044',	'\u0045', '\u0046',	'\u0047',	
		  '\u0048',	'\u0049', '\u004A',	'\u004B', '\u004C',	'\u004D', '\u004E',	'\u004F',	
		  '\u0050',	'\u0051', '\u0052',	'\u0053', '\u0054',	'\u0055', '\u0056',	'\u0057',	
		  '\u0058',	'\u0059', '\u005A',	'\u005B', '\\',     '\u005D', '\u2015',	'\u005F',	
		  '\u2551',	'\u0061', '\u0062',	'\u0063', '\u0064',	'\u0065', '\u0066',	'\u0067',	
		  '\u0068',	'\u0069', '\u006A',	'\u006B', '\u006C',	'\u006D', '\u006E',	'\u006F',	
		  '\u0070',	'\u0071', '\u0072',	'\u0073', '\u0074',	'\u0075', '\u0076',	'\u0077',	
		  '\u0078',	'\u0079', '\u007A',	'\u007B', '\u007C',	'\u007D', '\u00AF',	'\u007F',	
		  '\u00E1',	'\u00E0', '\u00E9',	'\u00E8', '\u00ED',	'\u00EC', '\u00F3',	'\u00F2',	
		  '\u00FA',	'\u00F9', '\u00D1',	'\u00C7', '\u015E',	'\u00DF', '\u00A1',	'\u0132',	
		  '\u00E2',	'\u00E4', '\u00EA',	'\u00EB', '\u00EE',	'\u00EF', '\u00F4',	'\u00F6',	
		  '\u00FB',	'\u00FC', '\u00F1',	'\u00E7', '\u015F',	'\u011F', '\u0131',	'\u0133',	
		  '\u00AA',	'\u03B1', '\u00A9',	'\u2030', '\u011E',	'\u011B', '\u0148',	'\u0151',	
		  '\u03C0',	'\u20AC', '\u00A3',	'\u0024', '\u2190',	'\u2191', '\u2192',	'\u2193',	
		  '\u00BA', '\u00B9', '\u00B2',	'\u00B3', '\u00B1',	'\u0130', '\u0144',	'\u0171',	
		  '\u00B5',	'\u00BF', '\u00F7', '\u00B0', '\u00BC',	'\u00BD', '\u00BE',	'\u00A7',	
		  '\u00C1',	'\u00C0', '\u00C9',	'\u00C8', '\u00CD',	'\u00CC', '\u00D3',	'\u00D2',	
		  '\u00DA',	'\u00D9', '\u0158',	'\u010C', '\u0160',	'\u017D', '\u0110',	'\u013F',	
		  '\u00C2',	'\u00C4', '\u00CA',	'\u00CB', '\u00CE',	'\u00CF', '\u00D4',	'\u00D6',	
		  '\u00DB',	'\u00DC', '\u0159',	'\u010D', '\u0161',	'\u017E', '\u0111',	'\u0140',	
		  '\u00C3',	'\u00C5', '\u00C6', '\u0152', '\u0177',	'\u00DD', '\u00D5',	'\u00D8',	
		  '\u00DE',	'\u014A', '\u0154',	'\u0106', '\u015A',	'\u0179', '\u0166',	'\u00F0',	
		  '\u00E3',	'\u00E5', '\u00E6',	'\u0153', '\u0175',	'\u00FD', '\u00F5',	'\u00F8',	
		  '\u00FE',	'\u014B', '\u0155',	'\u0107', '\u015B',	'\u017A', '\u0167',	'\u0020',
	};

	
	public static char toChar(int code) {
		return charmap[code];
	}
	
	public static float RDS_BITRATE = 1187.5f;
	
	public static String[] rdsPtyLabels = {
		"None/Undefined",
		"News",
		"Current Affairs",
		"Information",
		"Sport",
		"Education",
		"Drama",
		"Culture",
		"Science",
		"Varied",
		"Pop Music",
		"Rock Music",
		"Easy Listening Music",
		"Light classical",
		"Serious classical",
		"Other Music",
		"Weather",
		"Finance",
		"Children's programmes",
		"Social Affairs",
		"Religion",
		"Phone In",
		"Travel",
		"Leisure",
		"Jazz Music",
		"Country Music",
		"National Music",
		"Oldies Music",
		"Folk Music",
		"Documentary",
		"Alarm Test",
		"Alarm"
	};

	public static String[] rbdsPtyLabels = {
		"No program type or undefined",
		"News",
		"Information",
		"Sport",
		"Talk",
		"Rock",
		"Classic Rock",
		"Adult Hits",
		"Soft Rock",
		"Top 40",
		"Country",
		"Oldies",
		"Soft",
		"Nostalgia",
		"Jazz",
		"Classical",
		"Rhythm and Blues",
		"Soft Rhythm and Blues",
		"Foreign Language",
		"Religious Music",
		"Religious Talk",
		"Personality",
		"Public",
		"College",
		"Unassigned",
		"Unassigned",
		"Unassigned",
		"Unassigned",
		"Unassigned",
		"Weather",
		"Emergency Test",
		"Emergency"
	};
}
