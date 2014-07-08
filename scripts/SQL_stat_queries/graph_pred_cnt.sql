WITH Q0 AS ( SELECT T.GID AS GRAPH, T.ENTRY as ENTRY, LT.PROP AS PREDICATE, LT.VAL AS VALUE FROM 
DIRECT_PRIMARY_HASH AS T,TABLE(VALUES (T.PROP0,T.VAL0),(T.PROP1,T.VAL1),(T.PROP2,T.VAL2),(T.PROP3,T.VAL3),(T.PROP4,T.VAL4),(T.PROP5,T.VAL5),(T.PROP6,T.VAL6),(T.PROP7,T.VAL7),(T.PROP8,T.VAL8),(T.PROP9,T.VAL9),(T.PROP10,T.VAL10),(T.PROP11,T.VAL11),(T.PROP12,T.VAL12),(T.PROP13,T.VAL13),(T.PROP14,T.VAL14),(T.PROP15,T.VAL15),(T.PROP16,T.VAL16),(T.PROP17,T.VAL17),(T.PROP18,T.VAL18),(T.PROP19,T.VAL19),(T.PROP20,T.VAL20),(T.PROP21,T.VAL21),(T.PROP22,T.VAL22),(T.PROP23,T.VAL23),(T.PROP24,T.VAL24),(T.PROP25,T.VAL25),(T.PROP26,T.VAL26),(T.PROP27,T.VAL27),(T.PROP28,T.VAL28),(T.PROP29,T.VAL29),(T.PROP30,T.VAL30),(T.PROP31,T.VAL31),(T.PROP32,T.VAL32),(T.PROP33,T.VAL33),(T.PROP34,T.VAL34),(T.PROP35,T.VAL35),(T.PROP36,T.VAL36),(T.PROP37,T.VAL37),(T.PROP38,T.VAL38),(T.PROP39,T.VAL39),(T.PROP40,T.VAL40),(T.PROP41,T.VAL41),(T.PROP42,T.VAL42),(T.PROP43,T.VAL43),(T.PROP44,T.VAL44),(T.PROP45,T.VAL45),(T.PROP46,T.VAL46),(T.PROP47,T.VAL47),(T.PROP48,T.VAL48),(T.PROP49,T.VAL49),(T.PROP50,T.VAL50),(T.PROP51,T.VAL51),(T.PROP52,T.VAL52),(T.PROP53,T.VAL53),(T.PROP54,T.VAL54),(T.PROP55,T.VAL55),(T.PROP56,T.VAL56),(T.PROP57,T.VAL57),(T.PROP58,T.VAL58),(T.PROP59,T.VAL59),(T.PROP60,T.VAL60),(T.PROP61,T.VAL61),(T.PROP62,T.VAL62),(T.PROP63,T.VAL63),(T.PROP64,T.VAL64),(T.PROP65,T.VAL65),(T.PROP66,T.VAL66),(T.PROP67,T.VAL67),(T.PROP68,T.VAL68),(T.PROP69,T.VAL69),(T.PROP70,T.VAL70),(T.PROP71,T.VAL71),(T.PROP72,T.VAL72),(T.PROP73,T.VAL73),(T.PROP74,T.VAL74),(T.PROP75,T.VAL75),(T.PROP76,T.VAL76),(T.PROP77,T.VAL77),(T.PROP78,T.VAL78),(T.PROP79,T.VAL79),(T.PROP80,T.VAL80),(T.PROP81,T.VAL81),(T.PROP82,T.VAL82),(T.PROP83,T.VAL83),(T.PROP84,T.VAL84),(T.PROP85,T.VAL85),(T.PROP86,T.VAL86),(T.PROP87,T.VAL87),(T.PROP88,T.VAL88),(T.PROP89,T.VAL89),(T.PROP90,T.VAL90),(T.PROP91,T.VAL91),(T.PROP92,T.VAL92),(T.PROP93,T.VAL93),(T.PROP94,T.VAL94),(T.PROP95,T.VAL95),(T.PROP96,T.VAL96),(T.PROP97,T.VAL97),(T.PROP98,T.VAL98),(T.PROP99,T.VAL99),(T.PROP100,T.VAL100),(T.PROP101,T.VAL101),(T.PROP102,T.VAL102),(T.PROP103,T.VAL103),(T.PROP104,T.VAL104),(T.PROP105,T.VAL105),(T.PROP106,T.VAL106),(T.PROP107,T.VAL107),(T.PROP108,T.VAL108),(T.PROP109,T.VAL109),(T.PROP110,T.VAL110),(T.PROP111,T.VAL111),(T.PROP112,T.VAL112),(T.PROP113,T.VAL113),(T.PROP114,T.VAL114),(T.PROP115,T.VAL115),(T.PROP116,T.VAL116),(T.PROP117,T.VAL117),(T.PROP118,T.VAL118),(T.PROP119,T.VAL119),(T.PROP120,T.VAL120),(T.PROP121,T.VAL121),(T.PROP122,T.VAL122),(T.PROP123,T.VAL123),(T.PROP124,T.VAL124),(T.PROP125,T.VAL125),(T.PROP126,T.VAL126),(T.PROP127,T.VAL127)) AS LT(PROP,VAL) 
 WHERE LT.PROP IS NOT NULL),
Q1 AS ( SELECT T.GRAPH AS GRAPH, T.ENTRY AS ENTRY, T.PREDICATE AS PREDICATE,COALESCE(S.ELEM,T.VALUE) AS VALUE 
 FROM DIRECT_SECONDARY as S RIGHT OUTER JOIN Q0 AS T ON S.LIST_ID=T.VALUE AND S.ELEM IS NOT NULL)
SELECT GRAPH,PREDICATE, COUNT(*) AS CNT FROM Q1 GROUP BY GRAPH,PREDICATE ORDER BY CNT DESC FETCH FIRST 5000 ROWS ONLY;