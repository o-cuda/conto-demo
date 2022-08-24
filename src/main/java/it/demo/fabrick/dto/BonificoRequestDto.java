package it.demo.fabrick.dto;

import io.vertx.core.json.JsonObject;
import lombok.Data;

@Data
public class BonificoRequestDto {

	public Creditor creditor;
	public String executionDate;
	public String uri;
	public String description;
	public long amount;
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

		// todo mettere formatter della data odierna
		executionDate = "2022-08-24";
		uri = "REMITTANCE_INFORMATION";
		description = messageIn.getString("description");
		amount = Long.valueOf(messageIn.getString("amount"));
		currency = messageIn.getString("currency");
		isUrgent = false;
		isInstant = false;
		feeType = messageIn.getString("feeType");
		feeAccountId = messageIn.getString("feeAccountId");

		taxRelief = new TaxRelief();
		taxRelief.setTaxReliefId(messageIn.getString("taxReliefId"));
		taxRelief.setCondoUpgrade(Boolean.valueOf(messageIn.getString("isCondoUpgrade")));
		taxRelief.setCreditorFiscalCode(messageIn.getString("creditorFiscalCode"));
		taxRelief.setBeneficiaryType(messageIn.getString("beneficiaryType"));

		taxRelief.setNaturalPersonBeneficiary(new NaturalPersonBeneficiary());
		taxRelief.getNaturalPersonBeneficiary().setFiscalCode1(messageIn.getString("fiscalCode"));
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
