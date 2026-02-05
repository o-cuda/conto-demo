--- ********************************************************
--- **********     CONFIGURAZIONI                 **********
--- ********************************************************
 
INSERT INTO CONTO_CONFIGURATION (OPERATION, MESSAGE_IN, MESSAGE_OUT_BUS)
VALUES (
	'SAL',
	'OPERAZIONE=3;',
	'saldo_bus'
);

INSERT INTO CONTO_CONFIGURATION (OPERATION, MESSAGE_IN, MESSAGE_OUT_BUS)
VALUES (
	'LIS',
	'OPERAZIONE=3;start-date=10;end-date=10;',
	'lista_bus'
);

INSERT INTO CONTO_CONFIGURATION (OPERATION, MESSAGE_IN, MESSAGE_OUT_BUS)
VALUES (
	'BON', 'OPERAZIONE=3;creditor-name=50;accountCode=27;bicCode=11;description=500;amount=NUM20;currency=3;feeType=3;',
	'bonifico_bus'
);

--- ********************************************************
--- **********     INDIRIZZI PER AMBIENTE         **********
--- ********************************************************


INSERT INTO CONTO_INDIRIZZI (OPERATION, AMBIENTE, INDIRIZZO) 
VALUES 
	('SAL', 'SVIL', 'https://sandbox.platfr.io/api/gbs/banking/v4.0/accounts/{account-number}/balance'),
	('SAL', 'PRE-PROD', 'N.D.'),
	('SAL', 'PROD', 'N.D.');

INSERT INTO CONTO_INDIRIZZI (OPERATION, AMBIENTE, INDIRIZZO) 
VALUES 
	('LIS', 'SVIL', 'https://sandbox.platfr.io/api/gbs/banking/v4.0/accounts/{account-number}/transactions?fromAccountingDate={start-date}&toAccountingDate={end-date}'),
	('LIS', 'PRE-PROD', 'N.D.'),
	('LIS', 'PROD', 'N.D.');

INSERT INTO CONTO_INDIRIZZI (OPERATION, AMBIENTE, INDIRIZZO) 
VALUES 
	('BON', 'SVIL', 'https://sandbox.platfr.io/api/gbs/banking/v4.0/accounts/{account-number}/payments/money-transfers'),
	('BON', 'PRE-PROD', 'N.D.'),
	('BON', 'PROD', 'N.D.');
	
	
	 