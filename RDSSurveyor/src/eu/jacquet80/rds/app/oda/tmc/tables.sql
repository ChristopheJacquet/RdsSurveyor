-- 1 - Countries - COUNTRIES.DAT;
create table if not exists Countries(
  CID integer,
  ECC varchar(2),
  CCD varchar(1),
  NAME varchar(50));

-- 2 - LocationDataSets - LOCATIONDATASETS.DAT;
create table if not exists LocationDataSets(
  CID integer,
  TABCD integer,
  DCOMMENT varchar(100),
  VERSION varchar(7),
  VERSIONDESCRIPTION varchar(100));

-- 3 - Locationcodes - LOCATIONCODES.DAT; skipped for now

-- 4 - Classes - CLASSES.DAT; skipped for now

-- 5 - Types - TYPES.DAT; skipped for now

-- 6 - Subtypes - SUBTYPES.DAT; skipped for now

-- 7 - Languages - LANGUAGES.DAT; skipped for now

-- 8 - EuroRoadNo - EUROROADNO-DAT; skipped for now;

-- 9 - Names - NAMES.DAT;
create table if not exists Names(
  CID integer,
  LID integer,
  NID integer,
  NAME varchar(100),
  NCOMMENT varchar(100));

-- 10 -NameTranslations - NAMETRANSLATIONS.DAT; skipped for now

-- 11 - SubtypeTranslations - SUBTYPETRANSLATIONS.DAT; skipped for now

-- 12 - ERNo_belongs_to_country - ERNO_BELONGS_TO_CO.DAT; skipped for now

-- 13 - AdministrativeAreas - ADMINISTRATIVEAREA.DAT;
create table if not exists AdministrativeAreas(
  CID integer,
  TABCD integer,
  LCD integer,
  CLASS varchar(1),
  TCD integer,
  STCD integer,
  NID integer,
  POL_LCD integer);

-- 14 - OtherAreas - OTHERAREAS.DAT;
create table if not exists OtherAreas(
  CID integer,
  TABCD integer,
  LCD integer,
  CLASS varchar(1),
  TCD integer,
  STCD integer,
  NID integer,
  POL_LCD integer);

-- 15 - Roads - ROADS.DAT;
create table if not exists Roads(
  CID integer,
  TABCD integer,
  LCD integer,
  CLASS varchar(1),
  TCD integer,
  STCD integer,
  ROADNUMBER varchar(10),
  RNID integer,
  N1ID integer,
  N2ID integer,
  POL_LCD integer,
  PES_LEV integer,
  RDID integer);

-- 16 - Road_network_level_types - ROAD_NETWORK_LEVEL_TYPES.DAT; skipped for now

-- 17 - Segments - SEGMENTS.DAT;
create table if not exists Segments(
  CID integer,
  TABCD integer,
  LCD integer,
  CLASS varchar(1),
  TCD integer,
  STCD integer,
  ROADNUMBER varchar(10),
  RNID integer,
  N1ID integer,
  N2ID integer,
  ROA_LCD integer,
  SEG_LCD integer,
  POL_LCD integer,
  RDID integer);

-- 18 - Soffsets - SOFFSETS.DAT
create table if not exists Soffsets(
  CID integer,
  TABCD integer,
  LCD integer,
  NEG_OFF_LCD integer,
  POS_OFF_LCD integer);

-- 19 - Seg_has_ERNo - SEG_HAS_ERNO.DAT; skipped for now

-- 20 - Points - POINTS.DAT;
create table if not exists Points(
  CID integer,
  TABCD integer,
  LCD integer,
  CLASS varchar(1),
  TCD integer,
  STCD integer,
  JUNCTIONNUMBER varchar(10),
  RNID integer,
  N1ID integer,
  N2ID integer,
  POL_LCD integer,
  OTH_LCD integer,
  SEG_LCD integer,
  ROA_LCD integer,
  INPOS integer,
  INNEG integer,
  OUTPOS integer,
  OUTNEG integer,
  PRESENTPOS integer,
  PRESENTNEG integer,
  DIVERSIONPOS varchar(10),
  DIVERSIONNEG varchar(10),
  XCOORD decimal(3,5),
  YCOORD decimal(2,5),
  INTERRUPTSROAD integer,
  URBAN boolean,
  JNID integer);

-- 21 - Poffsets - POFFSETS.DAT
create table if not exists Poffsets(
  CID integer,
  TABCD integer,
  LCD integer,
  NEG_OFF_LCD integer,
  POS_OFF_LCD integer);

-- 22 - Intersections - INTERSECTIONS.DAT; skipped for now