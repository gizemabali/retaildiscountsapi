package retailerdiscountsapi.pojos;

/**
 * This is the class for userinfo index from elasticsearch
 * 
 * @author gizemabali
 *
 */
public class UserInfo {

	private String username;

	private String password;

	private String accountCreationDate;

	private boolean employee;

	private boolean affiliate;

	private boolean customer;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getAccountCreationDate() {
		return accountCreationDate;
	}

	public void setAccountCreationDate(String accountCreationDate) {
		this.accountCreationDate = accountCreationDate;
	}

	public boolean isEmployee() {
		return employee;
	}

	public void setEmployee(boolean employee) {
		this.employee = employee;
	}

	public boolean isAffiliate() {
		return affiliate;
	}

	public void setAffiliate(boolean affiliate) {
		this.affiliate = affiliate;
	}

	public boolean isCustomer() {
		return customer;
	}

	public void setCustomer(boolean customer) {
		this.customer = customer;
	}
}
