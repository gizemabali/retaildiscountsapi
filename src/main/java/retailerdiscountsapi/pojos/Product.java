package retailerdiscountsapi.pojos;

/**
 * This is the class for product index from elasticsearch
 * 
 * @author gizemabali
 *
 */
public class Product {

	private String productName;

	private String type;

	private long price;

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public long getPrice() {
		return price;
	}

	public void setPrice(long price) {
		this.price = price;
	}

}
