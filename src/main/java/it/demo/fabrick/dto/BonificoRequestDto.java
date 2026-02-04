package it.demo.fabrick.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.vertx.core.json.JsonObject;
import it.demo.fabrick.utils.MessageParserUtil;
import lombok.Data;

@Data
public class BonificoRequestDto {

	public static final String REMITTANCE_INFORMATION_URI = "REMITTANCE_INFORMATION";

	public Creditor creditor;
	public String executionDate;
	public String uri;
	public String description;
	public BigDecimal amount;
	public String currency;
	public boolean isUrgent;
	public boolean isInstant;
	public String feeType;
	public String feeAccountId;
	public TaxRelief taxRelief;

	public BonificoRequestDto(JsonObject messageIn) {
		this.creditor = new Creditor();
		creditor.setName(messageIn.getString("creditor-name"));
		creditor.setAccount(new Account());

		creditor.getAccount().setAccountCode(messageIn.getString("accountCode"));
		creditor.getAccount().setBicCode(messageIn.getString("bicCode"));

		LocalDate today = LocalDate.now();
		executionDate = today.toString();
		uri = REMITTANCE_INFORMATION_URI;
		description = messageIn.getString("description");

		// Parse amount as BigDecimal for precise monetary calculations
		String amountStr = messageIn.getString("amount");
		try {
			amount = new BigDecimal(amountStr);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid amount format: " + amountStr, e);
		}

		currency = messageIn.getString("currency");
		isUrgent = false;
		isInstant = false;
		feeType = messageIn.getString("feeType");
		feeAccountId = messageIn.getString("feeAccountId");
		if ( feeAccountId != null && feeAccountId.isBlank() ) {
			feeAccountId = null;
		}

		taxRelief = new TaxRelief();
		taxRelief.setTaxReliefId(messageIn.getString("taxReliefId"));
		taxRelief.setCondoUpgrade(Boolean.valueOf(messageIn.getString("isCondoUpgrade")));
		taxRelief.setCreditorFiscalCode(messageIn.getString("creditorFiscalCode"));
		taxRelief.setBeneficiaryType(messageIn.getString("beneficiaryType"));

		taxRelief.setNaturalPersonBeneficiary(new NaturalPersonBeneficiary());
		taxRelief.getNaturalPersonBeneficiary().setFiscalCode1(messageIn.getString("fiscalCode"));

		// Validate all request parameters
		MessageParserUtil.validateMoneyTransferRequest(amount, currency, creditor.getName(), description);
	}

	@Data
	public class Account {
		public String accountCode;
		public String bicCode;
	}

	@Data
	public class Address {
		public Object address;
		public Object city;
		public Object countryCode;
	}

	@Data
	public class Creditor {
		public String name;
		public Account account;
		public Address address;
	}

	@Data
	public class LegalPersonBeneficiary {
		public Object fiscalCode;
		public Object legalRepresentativeFiscalCode;
	}

	@Data
	public class NaturalPersonBeneficiary {
		public String fiscalCode1;
		public Object fiscalCode2;
		public Object fiscalCode3;
		public Object fiscalCode4;
		public Object fiscalCode5;
	}

	@Data
	public class TaxRelief {
		public String taxReliefId;
		public boolean isCondoUpgrade;
		public String creditorFiscalCode;
		public String beneficiaryType;
		public NaturalPersonBeneficiary naturalPersonBeneficiary;
		public LegalPersonBeneficiary legalPersonBeneficiary;
	}


}
